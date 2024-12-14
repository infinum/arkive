package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
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
) : KotlinSpec {
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

    fun generatePreviewFunction(holder: ComposeHolder): FunSpec {

        val builder = FunSpec.builder(holder.functionName)
            .addParameter(createRunnerParameter())

        val functionComponent = getComponentMember(holder)
        val id = holder.functionId
        builder.addCode(generateBaseVariant(id, functionComponent))

        generateFontVariants(id, functionComponent).forEach {
            builder.addCode(it)
        }

        generateDensityVariants(id, functionComponent).forEach {
            builder.addCode(it)
        }

        generateLayoutDirectionVariants(id, functionComponent).forEach {
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

    private fun generateBaseVariant(id: String, componentMember: MemberName): CodeBlock {
        return CodeBlock.builder().apply {
            addStatement("runner(%S) { %M() }", id, componentMember)
        }.build()
    }

    private fun generateFontVariants(id: String, componentMember: MemberName): List<CodeBlock> {
        val fontScales = listOf(1.0f, 1.5f, 2.0f)
        val fontVariantMember = MemberName(
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "FontVariant"
        )
        return fontScales.map { scale ->
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

    private fun generateDensityVariants(id: String, componentMember: MemberName): List<CodeBlock> {
        val densities = listOf(1.0f, 2.0f, 3.0f)
        val fontDensityMember = MemberName(
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "DensityVariant"
        )
        return densities.map { density ->
            CodeBlock.builder().apply {
                addStatement(
                    "runner(%S) { %M(scale = %Lf) { %M() } }",
                    "${id}_density_$density",
                    fontDensityMember,
                    density,
                    componentMember,
                )
            }.build()
        }
    }

    private fun generateLayoutDirectionVariants(
        id: String,
        componentMember: MemberName
    ): List<CodeBlock> {
        val layoutDirections = listOf("LTR" to true, "RTL" to false)
        val layoutDirectionMember = MemberName(
            packageName = "com.infinum.arkive.composeutils",
            simpleName = "LayoutDirectionVariant"
        )
        return layoutDirections.map { (directionName, isLtr) ->
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

    companion object {
        private const val SIMPLE_NAME = "ComposeVariants"
        private const val RUNNER_FUNCTION = "runner"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"

    }
}