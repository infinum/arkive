package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector.Companion.TAG_COMPOSABLE
import com.infinum.arkive.processor.collectors.ArkiveViewCollector.Companion.TAG_VIEW
import com.infinum.arkive.processor.models.UiComponentHolder
import com.infinum.arkive.processor.shared.Constants
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

class UiComponentSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<UiComponentHolder>,
    private val logger: KSPLogger,
) : KotlinSpec {
    override fun write() {
        val fileSpec = getFileSpec()
        val arkiveClass = getArkiveClass()
        arkiveClass.addFunction(getRunComposableTestsFunction())
        arkiveClass.addFunction(getRunViewTestsFunction())

        fileSpec
            .addType(arkiveClass.build())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )

        logger.info("Generated $SIMPLE_NAME")
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    private fun getRunnerFunction(holder: UiComponentHolder): String {
        return """
            runner("${holder.functionId}") {
             ${getCodeBody(holder).toString().trimIndent()}
            } 
        """.trimIndent()
    }

    private fun getRunComposableTestsFunction(): FunSpec {
        return FunSpec.builder(RUN_COMPOSABLE_TESTS_FUNCTION)
            .addParameter(
                RUNNER_FUNCTION,
                LambdaTypeName.get(
                    parameters = arrayOf(
                        ClassName("kotlin", "String"),
                        LambdaTypeName.get(returnType = UNIT)
                            .copy(annotations = listOf(getComposableAnnotation())),
                    ),
                    returnType = UNIT,
                ),
            )
            .addCode(
                holders.filter { it.extraMetadata.contains(TAG_COMPOSABLE) }.joinToString(separator = "\n") { holder ->
                    getRunnerFunction(holder)
                },
            )
            .build()
    }

    private fun getRunViewTestsFunction(): FunSpec {
        return FunSpec.builder(RUN_VIEW_TESTS_FUNCTION)
            .addParameter(
                RUNNER_FUNCTION,
                LambdaTypeName.get(
                    parameters = arrayOf(
                        ClassName("kotlin", "String"),
                        LambdaTypeName.get(returnType = INT)
                    ),
                    returnType = UNIT,
                ),
            )
            .addCode(
                holders.filter { it.extraMetadata.contains(TAG_VIEW) }.joinToString(separator = "\n") { holder ->
                    getRunnerFunction(holder)
                },
            )
            .build()
    }

    private fun getArkiveClass(): TypeSpec.Builder = TypeSpec.classBuilder(SIMPLE_NAME)

    private fun getCodeBody(holder: UiComponentHolder): CodeBlock {
        return CodeBlock.builder().apply {
            val functionMember = MemberName(
                packageName = holder.packageName,
                simpleName = holder.functionName,
            )
            addStatement(
                "%M()",
                functionMember,
            )
        }.build()
    }

    private fun getComposableAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName.bestGuess(ANNOTATION_COMPOSABLE))
            .build()
    }

    companion object {
        private const val SIMPLE_NAME = "ArkiveShoot"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val RUN_COMPOSABLE_TESTS_FUNCTION = "runComposableTests"
        private const val RUN_VIEW_TESTS_FUNCTION = "runViewTests"
        private const val RUNNER_FUNCTION = "runner"
    }
}
