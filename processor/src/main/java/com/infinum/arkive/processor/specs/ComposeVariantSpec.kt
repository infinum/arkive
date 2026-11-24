package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.models.PreviewTheme
import com.infinum.arkive.processor.shared.Constants
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

internal class ComposeVariantSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
    private val enablePreviewParameters: Boolean,
    private val enableVariants: Boolean,
    // Skin order: the first is the default every snapshot renders in; empty without a theme.
    private val previewThemes: List<PreviewTheme>,
) : Spec {

    // Chunked into several files: the Compose compiler hoists every capture-free composable
    // lambda of a file into that file's ComposableSingletons, whose static initializer hits
    // the JVM's 64KB method limit on large modules (MethodTooLargeException). Sorted so the
    // chunking is stable across builds.
    override fun write() {
        holders.sortedBy { it.functionId }
            .chunked(COMPONENTS_PER_FILE)
            .forEachIndexed { index, chunk ->
                val fileSpec = FileSpec.builder(Constants.PACKAGE_NAME, "$SIMPLE_NAME$index")
                chunk.forEach { holder ->
                    fileSpec.addFunction(generatePreviewFunction(holder))
                    fileSpec.addFunction(generatePreviewVariantsFunction(holder))
                }

                fileSpec
                    .build()
                    .writeTo(
                        codeGenerator = codeGenerator,
                        aggregating = true,
                        originatingKSFiles = chunk.mapNotNull { it.function.containingFile }.distinct(),
                    )
            }
    }

    // Wrappers are named by the unique component id, not the bare function name — previews
    // with the same name in different packages would otherwise generate clashing overloads.
    // Ids are dash-joined, so KotlinPoet emits the wrapper names backtick-escaped.
    // Base and variants are split so golden testing can run against base snapshots only.

    private fun generatePreviewFunction(holder: ComposeHolder): FunSpec {
        return FunSpec.builder(holder.functionId)
            .addParameter(createRunnerParameter())
            .addCode(generateBaseVariant(holder.functionId, getComponentMember(holder), holder))
            .build()
    }

    private fun generatePreviewVariantsFunction(holder: ComposeHolder): FunSpec {
        val builder = FunSpec.builder("${holder.functionId}$VARIANTS_SUFFIX")
            .addParameter(createRunnerParameter())

        val functionComponent = getComponentMember(holder)
        val id = holder.functionId

        if (enablePreviewParameters) {
            generatePreviewVariants(holder, id, functionComponent)?.let {
                builder.addCode(it)
            }
        }

        if (enableVariants) {
            generateFontVariants(id, functionComponent, holder).forEach {
                builder.addCode(it)
            }

            generateDensityVariants(id, functionComponent, holder).forEach {
                builder.addCode(it)
            }

            generateLayoutDirectionVariants(id, functionComponent, holder).forEach {
                builder.addCode(it)
            }

            generateUiModeVariants(id, functionComponent, holder).forEach {
                builder.addCode(it)
            }
        }

        return builder.build()
    }

    private fun createRunnerParameter(): ParameterSpec {
        return ParameterSpec.builder(
            RUNNER_FUNCTION,
            LambdaTypeName.get(
                parameters = arrayOf(
                    ClassName("kotlin", "String"),
                    LambdaTypeName.get(returnType = UNIT)
                        .copy(annotations = listOf(getComposableAnnotation())),
                ),
                returnType = UNIT,
            ),
        ).build()
    }

    private fun getComponentMember(holder: ComposeHolder): MemberName {
        return MemberName(
            packageName = holder.packageName,
            simpleName = holder.functionName,
        )
    }

    private fun getComposableAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName.bestGuess(ANNOTATION_COMPOSABLE))
            .build()
    }

    private fun generateBaseVariant(id: String, componentMember: MemberName, holder: ComposeHolder): CodeBlock {
        return CodeBlock.builder()
            .addStatement("runner(%S) { %L }", id, componentContent(holder, componentMember))
            .build()
    }

    private fun generatePreviewVariants(holder: ComposeHolder, id: String, componentMember: MemberName): CodeBlock? {
        val providerMember = extractPreviewParameterClass(holder)?.toMemberName() ?: return null

        // The parameter name becomes the variant category. Prefixed with "param-" so a
        // parameter named `font`/`density`/`layoutDirection` can't collide with the
        // built-in categories, and '_' is sanitized to '-' so the category never
        // introduces extra '_' separators into the snapshot filename.
        val parameterName = holder.parameters.firstOrNull()?.name?.asString().orEmpty()
        val parameterCategory = "param-${parameterName.replace('_', '-')}"
        return CodeBlock.builder()
            .addStatement(
                "%M().values.forEachIndexed { index, it -> runner(%L) { %L } }",
                providerMember,
                "\"${id}_${parameterCategory}_\${index}\"",
                componentContent(holder, componentMember, argument = CodeBlock.of("it")),
            )
            .build()
    }

    // The preview call every snapshot renders: the component (fed its first preview-parameter
    // value, if any) inside [theme] — the default skin unless a skin variant picks another.
    // [darkTheme] is passed to a theme that declares a dark-mode parameter; null leaves the
    // theme's default.
    private fun componentContent(
        holder: ComposeHolder,
        componentMember: MemberName,
        argument: CodeBlock? = firstPreviewParameterValue(holder),
        darkTheme: Boolean? = null,
        theme: PreviewTheme? = previewThemes.firstOrNull(),
    ): CodeBlock {
        val call = if (argument == null) {
            CodeBlock.of("%M()", componentMember)
        } else {
            CodeBlock.of("%M(%L)", componentMember, argument)
        }
        if (theme == null) {
            return call
        }
        val darkThemeValue = darkTheme ?: false.takeIf { theme.darkThemeRequired }
        val darkThemeArgument = theme.darkThemeParameter
            ?.let { parameter -> darkThemeValue?.let { "$parameter = $it" } }
        val arguments = theme.arguments + listOfNotNull(darkThemeArgument)
        val argumentList = if (arguments.isEmpty()) "" else arguments.joinToString(prefix = "(", postfix = ")")

        return CodeBlock.of("%M%L { %L }", theme.member, argumentList, call)
    }

    private fun firstPreviewParameterValue(holder: ComposeHolder): CodeBlock? {
        val providerMember = extractPreviewParameterClass(holder)?.toMemberName() ?: return null
        return CodeBlock.of("%M().values.first()", providerMember)
    }

    private fun Pair<String, String>.toMemberName(): MemberName =
        MemberName(packageName = first, simpleName = second)

    private fun extractPreviewParameterClass(holder: ComposeHolder): Pair<String, String>? {
        for (parameter in holder.parameters) {
            val previewAnnotation = parameter.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() in PREVIEW_PARAMETER_ANNOTATION_NAMES
            }

            if (previewAnnotation != null) {
                return extractClassFromAnnotation(previewAnnotation)
            }
        }
        return null
    }

    private fun extractClassFromAnnotation(annotation: KSAnnotation): Pair<String, String>? {
        val classArgument = annotation.arguments.find { it.name?.asString() == "provider" }
        val ksType = classArgument?.value as? KSType ?: return null

        return getClassDetailsFromKSType(ksType)
    }

    private fun getClassDetailsFromKSType(ksType: KSType): Pair<String, String>? {
        val declaration = ksType.declaration as? KSClassDeclaration ?: return null

        val qualifiedName = declaration.qualifiedName?.asString() ?: return null
        val simpleName = declaration.simpleName.asString()

        val packageName = if (qualifiedName.contains('.')) {
            qualifiedName.substringBeforeLast('.')
        } else {
            ""
        }

        return Pair(packageName, simpleName)
    }

    private fun generateFontVariants(id: String, componentMember: MemberName, holder: ComposeHolder): List<CodeBlock> {
        val fontScales = listOf(FONT_SCALE_NORMAL, FONT_SCALE_LARGE, FONT_SCALE_LARGEST)
        val fontVariantMember = MemberName(
            packageName = COMPOSE_UTILS_PACKAGE,
            simpleName = "FontVariant",
        )
        val content = componentContent(holder, componentMember)

        return fontScales.map { scale ->
            CodeBlock.builder()
                .addStatement(
                    "runner(%S) { %M(scale = %Lf) { %L } }",
                    "${id}_font_$scale",
                    fontVariantMember,
                    scale,
                    content,
                )
                .build()
        }
    }

    private fun generateDensityVariants(id: String, componentMember: MemberName, holder: ComposeHolder): List<CodeBlock> {
        val densities = mapOf(
            "ldpi" to DENSITY_LDPI,
            "mdpi" to DENSITY_MDPI,
            "hdpi" to DENSITY_HDPI,
            "xhdpi" to DENSITY_XHDPI,
            "xxhdpi" to DENSITY_XXHDPI,
            "xxxhdpi" to DENSITY_XXXHDPI,
        )
        val densityVariantMember = MemberName(
            packageName = COMPOSE_UTILS_PACKAGE,
            simpleName = "DensityVariant",
        )
        val content = componentContent(holder, componentMember)

        return densities.map { (density, value) ->
            CodeBlock.builder()
                .addStatement(
                    "runner(%S) { %M(scale = %Lf) { %L } }",
                    "${id}_density_$density",
                    densityVariantMember,
                    value,
                    content,
                )
                .build()
        }
    }

    private fun generateLayoutDirectionVariants(
        id: String,
        componentMember: MemberName,
        holder: ComposeHolder,
    ): List<CodeBlock> {
        val layoutDirections = mapOf("LTR" to true, "RTL" to false)
        val layoutDirectionMember = MemberName(
            packageName = COMPOSE_UTILS_PACKAGE,
            simpleName = "LayoutDirectionVariant",
        )
        val content = componentContent(holder, componentMember)

        return layoutDirections.map { (directionName, isLtr) ->
            CodeBlock.builder()
                .addStatement(
                    "runner(%S) { %M(isLtr = %L) { %L } }",
                    "${id}_layoutDirection_$directionName",
                    layoutDirectionMember,
                    isLtr,
                    content,
                )
                .build()
        }
    }

    // Light and dark for every skin: uiMode_<mode> for a module with at most one theme,
    // skin_<name>-<mode> for every skin (the default included) once there are several, so
    // all colorways sit in one category. UiModeVariant outermost, so a theme defaulting to
    // isSystemInDarkTheme() — including one a preview applies itself — already sees the
    // flipped configuration.
    private fun generateUiModeVariants(id: String, componentMember: MemberName, holder: ComposeHolder): List<CodeBlock> {
        val uiModes = mapOf(UI_MODE_LIGHT to false, UI_MODE_DARK to true)
        val uiModeVariantMember = MemberName(
            packageName = COMPOSE_UTILS_PACKAGE,
            simpleName = "UiModeVariant",
        )
        val shots = if (previewThemes.size <= 1) {
            listOf(previewThemes.firstOrNull() to "${id}_uiMode_")
        } else {
            previewThemes.map { it to "${id}_skin_${it.name}-" }
        }

        return shots.flatMap { (theme, prefix) ->
            uiModes.map { (uiModeName, isDark) ->
                CodeBlock.builder()
                    .addStatement(
                        "runner(%S) { %M(isDark = %L) { %L } }",
                        prefix + uiModeName,
                        uiModeVariantMember,
                        isDark,
                        componentContent(holder, componentMember, darkTheme = isDark, theme = theme),
                    )
                    .build()
            }
        }
    }

    companion object {
        private const val SIMPLE_NAME = "ComposeVariants"
        private const val COMPONENTS_PER_FILE = 10
        const val VARIANTS_SUFFIX = "_variants"
        private const val RUNNER_FUNCTION = "runner"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val COMPOSE_UTILS_PACKAGE = "com.infinum.arkive.composeutils"

        // Compose Multiplatform ships its own PreviewParameter under the jetbrains
        // namespace; a common preview may use either FQN.
        val PREVIEW_PARAMETER_ANNOTATION_NAMES = setOf(
            "androidx.compose.ui.tooling.preview.PreviewParameter",
            "org.jetbrains.compose.ui.tooling.preview.PreviewParameter",
        )

        private const val FONT_SCALE_NORMAL = 1.0f
        private const val FONT_SCALE_LARGE = 1.5f
        private const val FONT_SCALE_LARGEST = 2.0f

        private const val DENSITY_LDPI = 0.75f
        private const val DENSITY_MDPI = 1.0f
        private const val DENSITY_HDPI = 1.5f
        private const val DENSITY_XHDPI = 2.0f
        private const val DENSITY_XXHDPI = 3.0f
        private const val DENSITY_XXXHDPI = 4.0f

        private const val UI_MODE_LIGHT = "light"
        private const val UI_MODE_DARK = "dark"
    }
}
