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

class ComposeVariantSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
    private val enablePreviewParameters: Boolean,
    private val enableVariants: Boolean,
) : KotlinSpec {

    override fun write() {
        val fileSpec = getFileSpec()
        holders.forEach { holder ->
            fileSpec.addFunction(generatePreviewFunction(holder))
            fileSpec.addFunction(generatePreviewVariantsFunction(holder))
        }

        fileSpec
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = holders.mapNotNull { it.function.containingFile }.distinct(),
            )
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    // Wrappers are named by the unique component id, not the bare function name — previews
    // with the same name in different packages would otherwise generate clashing overloads.
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
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)
        return if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )

            CodeBlock.builder().apply {
                addStatement(
                    "runner(%S) { %M(%M().values.first()) }",
                    id,
                    componentMember,
                    providerFunctionMember,
                )
            }.build()
        } else {
            CodeBlock.builder().apply {
                addStatement("runner(%S) { %M() }", id, componentMember)
            }.build()
        }
    }

    private fun generatePreviewVariants(holder: ComposeHolder, id: String, componentMember: MemberName): CodeBlock? {
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)

        if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )
            val parameterName = holder.parameters.firstOrNull()?.name?.asString().orEmpty()
            val provider = CodeBlock.builder().apply {
                addStatement(
                    "%M().values.forEachIndexed { index, it -> runner(%L) { %M(it) } }",
                    providerFunctionMember,
                    "\"${id}_${parameterName}_\${index}\"",
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
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAMETER_ANNOTATION_NAME
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

        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)
        return if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )

            fontScales.map { scale ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(scale = %Lf) { %M(%M().values.first()) } }",
                        "${id}_font_$scale",
                        fontVariantMember,
                        scale,
                        componentMember,
                        providerFunctionMember,
                    )
                }.build()
            }
        } else {
            fontScales.map { scale ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(scale = %Lf) { %M() } }",
                        "${id}_font_$scale",
                        fontVariantMember,
                        scale,
                        componentMember,
                    )
                }.build()
            }
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
        val fontDensityMember = MemberName(
            packageName = COMPOSE_UTILS_PACKAGE,
            simpleName = "DensityVariant",
        )
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)
        return if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )
            densities.map { (density, value) ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(scale = %Lf) { %M(%M().values.first()) } }",
                        "${id}_density_$density",
                        fontDensityMember,
                        value,
                        componentMember,
                        providerFunctionMember,
                    )
                }.build()
            }
        } else {
            densities.map { (density, value) ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(scale = %Lf) { %M() } }",
                        "${id}_density_$density",
                        fontDensityMember,
                        value,
                        componentMember,
                    )
                }.build()
            }
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
        val providerInfo: Pair<String, String>? = extractPreviewParameterClass(holder)
        return if (providerInfo != null) {
            val (packageName, simpleName) = providerInfo
            val providerFunctionMember = MemberName(
                packageName = packageName,
                simpleName = simpleName,
            )
            layoutDirections.map { (directionName, isLtr) ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(isLtr = %L) { %M(%M().values.first()) } }",
                        "${id}_layoutDirection_$directionName",
                        layoutDirectionMember,
                        isLtr,
                        componentMember,
                        providerFunctionMember,
                    )
                }.build()
            }
        } else {
            layoutDirections.map { (directionName, isLtr) ->
                CodeBlock.builder().apply {
                    addStatement(
                        "runner(%S) { %M(isLtr = %L) { %M() } }",
                        "${id}_layoutDirection_$directionName",
                        layoutDirectionMember,
                        isLtr,
                        componentMember,
                    )
                }.build()
            }
        }
    }

    companion object {
        private const val SIMPLE_NAME = "ComposeVariants"
        const val VARIANTS_SUFFIX = "_variants"
        private const val RUNNER_FUNCTION = "runner"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val COMPOSE_UTILS_PACKAGE = "com.infinum.arkive.composeutils"
        const val PREVIEW_PARAMETER_ANNOTATION_NAME = "androidx.compose.ui.tooling.preview.PreviewParameter"

        private const val FONT_SCALE_NORMAL = 1.0f
        private const val FONT_SCALE_LARGE = 1.5f
        private const val FONT_SCALE_LARGEST = 2.0f

        private const val DENSITY_LDPI = 0.75f
        private const val DENSITY_MDPI = 1.0f
        private const val DENSITY_HDPI = 1.5f
        private const val DENSITY_XHDPI = 2.0f
        private const val DENSITY_XXHDPI = 3.0f
        private const val DENSITY_XXXHDPI = 4.0f
    }
}
