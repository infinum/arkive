package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.infinum.arkive.processor.models.ComposeHolder
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
) : KotlinSpec {

    override fun write() {
        fileSpec().writeTo(
            codeGenerator = codeGenerator,
            aggregating = true,
            originatingKSFiles = holders.mapNotNull { it.function.containingFile }.distinct(),
        )
    }

    internal fun fileSpec(): FileSpec {
        val fileSpec = getFileSpec()
        holders.forEach { holder ->
            fileSpec.addFunction(generatePreviewFunction(holder))
            fileSpec.addFunction(generatePreviewVariantsFunction(holder))
        }
        return fileSpec.build()
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    // Wrappers are named by the unique component id, not the bare function name — previews
    // with the same name in different packages would otherwise generate clashing overloads.
    // Ids are dash-joined, so KotlinPoet emits the wrapper names backtick-escaped.
    // Base and variants are split so golden testing can run against base snapshots only.

    private fun generatePreviewFunction(holder: ComposeHolder): FunSpec {
        return FunSpec.builder(holder.functionId)
            .addParameter(ParameterSpec.builder(RUNNER_FUNCTION, runnerType()).build())
            .addCode(generateBaseVariant(holder.functionId, getComponentMember(holder), holder))
            .build()
    }

    private fun generatePreviewVariantsFunction(holder: ComposeHolder): FunSpec {
        val builder = FunSpec.builder("${holder.functionId}$VARIANTS_SUFFIX")
            .addParameter(ParameterSpec.builder(RUNNER_FUNCTION, runnerType()).build())

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
        }

        return builder.build()
    }

    private fun getComponentMember(holder: ComposeHolder): MemberName {
        return MemberName(
            packageName = holder.packageName,
            simpleName = holder.functionName,
        )
    }

    // The component call every shot renders: a preview with a @PreviewParameter gets its
    // provider's first value.
    private fun componentCall(holder: ComposeHolder, componentMember: MemberName): CodeBlock {
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)
        return if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )
            CodeBlock.of("%M(%M().values.first())", componentMember, providerFunctionMember)
        } else {
            CodeBlock.of("%M()", componentMember)
        }
    }

    private fun generateBaseVariant(id: String, componentMember: MemberName, holder: ComposeHolder): CodeBlock {
        return CodeBlock.builder()
            .addStatement("runner(%S, %T.Default) { %L }", id, SHOT_CONFIG, componentCall(holder, componentMember))
            .build()
    }

    private fun generatePreviewVariants(holder: ComposeHolder, id: String, componentMember: MemberName): CodeBlock? {
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)

        if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )
            // The parameter name becomes the variant category. Prefixed with "param-" so a
            // parameter named `font`/`density`/`layoutDirection` can't collide with the
            // built-in categories, and '_' is sanitized to '-' so the category never
            // introduces extra '_' separators into the snapshot filename.
            val parameterName = holder.parameters.firstOrNull()?.name?.asString().orEmpty()
            val parameterCategory = "param-${parameterName.replace('_', '-')}"
            val provider = CodeBlock.builder().apply {
                addStatement(
                    "%M().values.forEachIndexed { index, it -> runner(%L, %T.Default) { %M(it) } }",
                    providerFunctionMember,
                    "\"${id}_${parameterCategory}_\${index}\"",
                    SHOT_CONFIG,
                    componentMember,
                )
            }.build()

            return provider
        } else {
            return null
        }
    }

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

    // Font scale and density are device settings: the shot asks its engine for them through
    // ArkiveShotConfig instead of overriding LocalDensity (see ArkiveShotConfig for why).

    private fun generateFontVariants(id: String, componentMember: MemberName, holder: ComposeHolder): List<CodeBlock> {
        val fontScales = listOf(FONT_SCALE_NORMAL, FONT_SCALE_LARGE, FONT_SCALE_LARGEST)
        val call = componentCall(holder, componentMember)
        return fontScales.map { scale ->
            CodeBlock.builder()
                .addStatement("runner(%S, %T(fontScale = %Lf)) { %L }", "${id}_font_$scale", SHOT_CONFIG, scale, call)
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
        val call = componentCall(holder, componentMember)
        return densities.map { (density, value) ->
            CodeBlock.builder()
                .addStatement("runner(%S, %T(density = %Lf)) { %L }", "${id}_density_$density", SHOT_CONFIG, value, call)
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
        val call = componentCall(holder, componentMember)
        return layoutDirections.map { (directionName, isLtr) ->
            CodeBlock.builder()
                .addStatement(
                    "runner(%S, %T.Default) { %M(isLtr = %L) { %L } }",
                    "${id}_layoutDirection_$directionName",
                    SHOT_CONFIG,
                    layoutDirectionMember,
                    isLtr,
                    call,
                )
                .build()
        }
    }

    companion object {
        private const val SIMPLE_NAME = "ComposeVariants"
        const val VARIANTS_SUFFIX = "_variants"
        private const val RUNNER_FUNCTION = "runner"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val COMPOSE_UTILS_PACKAGE = "com.infinum.arkive.composeutils"

        private val SHOT_CONFIG = ClassName(COMPOSE_UTILS_PACKAGE, "ArkiveShotConfig")

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

        /**
         * The shot callback every generated wrapper and the shooter take:
         * `(shotName, ArkiveShotConfig, @Composable content) -> Unit`. The engine's generated
         * test is the one implementing it, so the config reaches the engine.
         */
        fun runnerType(): LambdaTypeName = LambdaTypeName.get(
            parameters = arrayOf(
                ClassName("kotlin", "String"),
                SHOT_CONFIG,
                LambdaTypeName.get(returnType = UNIT)
                    .copy(annotations = listOf(AnnotationSpec.builder(ClassName.bestGuess(ANNOTATION_COMPOSABLE)).build())),
            ),
            returnType = UNIT,
        )
    }
}
