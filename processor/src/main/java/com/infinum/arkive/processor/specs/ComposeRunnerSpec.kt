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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

class ComposeRunnerSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
    private val logger: KSPLogger,
) : KotlinSpec {
    override fun write() {
        val fileSpec = getFileSpec()
        val arkiveClass = getArkiveClass()
        arkiveClass.addFunction(getRunTestsFunction(RUN_COMPOSABLE_TESTS_FUNCTION, wrapperSuffix = ""))
        arkiveClass.addFunction(
            getRunTestsFunction(
                RUN_COMPOSABLE_VARIANT_TESTS_FUNCTION,
                wrapperSuffix = ComposeVariantSpec.VARIANTS_SUFFIX,
            ),
        )

        fileSpec
            .addType(arkiveClass.build())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = holders.mapNotNull { it.function.containingFile }.distinct(),
            )

        logger.info("Generated $SIMPLE_NAME")
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    private fun getRunnerFunction(holder: ComposeHolder, wrapperSuffix: String): CodeBlock {
        val functionRunner = MemberName(
            packageName = "com.infinum.arkive",
            simpleName = "${holder.functionId}$wrapperSuffix",
        )
        // Each component (base or its variants) is isolated: a preview that fails to render
        // logs and is dropped from the showcase instead of aborting the whole snapshot run.
        return CodeBlock.builder().apply {
            beginControlFlow("try")
            addStatement("%M(runner)", functionRunner)
            nextControlFlow("catch (e: %T)", ClassName("kotlin", "Throwable"))
            addStatement(
                "println(%S + e.message)",
                "Arkive: skipping component ${holder.functionId}$wrapperSuffix, snapshot failed: ",
            )
            endControlFlow()
        }.build()
    }

    private fun getRunTestsFunction(functionName: String, wrapperSuffix: String): FunSpec {
        return FunSpec.builder(functionName)
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
            // CodeBlocks are added directly, never rendered to String and re-parsed:
            // addCode(String) treats '%' as format specifiers and bypasses MemberName
            // import handling.
            .addCode(
                CodeBlock.builder()
                    .apply { holders.forEach { add(getRunnerFunction(it, wrapperSuffix)) } }
                    .build(),
            )
            .build()
    }

    private fun getArkiveClass(): TypeSpec.Builder = TypeSpec.classBuilder(SIMPLE_NAME)

    private fun getComposableAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName.bestGuess(ANNOTATION_COMPOSABLE))
            .build()
    }

    companion object {
        private const val SIMPLE_NAME = "ArkiveComposeShoot"
        private const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val RUN_COMPOSABLE_TESTS_FUNCTION = "runComposableTests"
        private const val RUN_COMPOSABLE_VARIANT_TESTS_FUNCTION = "runComposableVariantTests"
        private const val RUNNER_FUNCTION = "runner"
    }
}
