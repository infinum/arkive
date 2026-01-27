package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
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
    private val logger: KSPLogger,
    disablePreviewParameters: Boolean,
) : KotlinSpec {
    private val enablePreviewParameters = !disablePreviewParameters

    override fun write() {
        val fileSpec = getFileSpec()
        holders.forEach { holder ->
            fileSpec.addFunction(generatePreviewFunction(holder))
        }

        fileSpec
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    private fun generatePreviewFunction(holder: ComposeHolder): FunSpec {

        val builder = FunSpec.builder(holder.functionName)
            .addParameter(createRunnerParameter())

        val functionComponent = getComponentMember(holder)
        val id = holder.functionId
        builder.addCode(generateBaseVariant(id, functionComponent, holder))

        if (enablePreviewParameters) {
            generatePreviewVariants(holder, id, functionComponent)?.let {
                builder.addCode(it)
            }
        }

        generateFontVariants(id, functionComponent, holder).forEach {
            builder.addCode(it)
        }

        generateDensityVariants(id, functionComponent, holder).forEach {
            builder.addCode(it)
        }

        generateLayoutDirectionVariants(id, functionComponent, holder).forEach {
            builder.addCode(it)
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
                addStatement("runner(%S) { %M(%M().values.first()) }",
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
        val fontScales = listOf(1.0f, 1.5f, 2.0f)
        val fontVariantMember = MemberName(
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "FontVariant"
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
                        "${id}_font_${scale}",
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
                        "${id}_font_${scale}",
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
            "ldpi" to 0.75f,
            "mdpi" to 1.0f,
            "hdpi" to 1.5f,
            "xhdpi" to 2.0f,
            "xxhdpi" to 3.0f,
            "xxxhdpi" to 4.0f
        )
        val fontDensityMember = MemberName(
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "DensityVariant"
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
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "LayoutDirectionVariant"
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
        private const val RUNNER_FUNCTION = "runner"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        const val PREVIEW_PARAMETER_ANNOTATION_NAME = "androidx.compose.ui.tooling.preview.PreviewParameter"
    }
}