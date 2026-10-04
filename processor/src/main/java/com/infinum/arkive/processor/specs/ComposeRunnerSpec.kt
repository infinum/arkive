package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.shared.Constants
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

class ComposeRunnerSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
    private val appPackages: Set<String>,
    private val logger: KSPLogger,
) : KotlinSpec {
    override fun write() {
        fileSpec().writeTo(
            codeGenerator = codeGenerator,
            aggregating = true,
            originatingKSFiles = holders.mapNotNull { it.function.containingFile }.distinct(),
        )

        logger.info("Generated $SIMPLE_NAME")
    }

    internal fun fileSpec(): FileSpec {
        val arkiveClass = getArkiveClass()
        arkiveClass.addFunction(getRunTestsFunction(RUN_COMPOSABLE_TESTS_FUNCTION, wrapperSuffix = ""))
        arkiveClass.addFunction(
            getRunTestsFunction(
                RUN_COMPOSABLE_VARIANT_TESTS_FUNCTION,
                wrapperSuffix = ComposeVariantSpec.VARIANTS_SUFFIX,
            ),
        )
        return getFileSpec().addType(arkiveClass.build()).build()
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
        // In verify mode Paparazzi signals a golden mismatch as an AssertionError from
        // snapshot() — those are collected (not swallowed) so verification can fail with an
        // aggregate report. Render crashes stay skip-logged in both modes: a preview that
        // never recorded has no golden, so verify must not fail on it.
        return CodeBlock.builder().apply {
            beginControlFlow("try")
            addStatement("%M($CAPTURING_RUNNER)", functionRunner)
            nextControlFlow("catch (e: %T)", ClassName(KOTLIN_PACKAGE, "AssertionError"))
            beginControlFlow("if ($IS_VERIFY_RUN_PROPERTY)")
            addStatement(
                "$VERIFY_FAILURES_PROPERTY += %S + (e.message ?: e.toString())",
                "${holder.functionId}$wrapperSuffix: ",
            )
            nextControlFlow("else")
            addStatement(
                "println(%S + (e.message ?: e.toString()))",
                "Arkive: skipping component ${holder.functionId}$wrapperSuffix, snapshot failed: ",
            )
            endControlFlow()
            nextControlFlow("catch (e: %T)", ClassName(KOTLIN_PACKAGE, "Throwable"))
            addStatement(
                "println(%S + (e.message ?: e.toString()))",
                "Arkive: skipping component ${holder.functionId}$wrapperSuffix, snapshot failed: ",
            )
            endControlFlow()
        }.build()
    }

    private fun getRunTestsFunction(functionName: String, wrapperSuffix: String): FunSpec {
        return FunSpec.builder(functionName)
            .addParameter(RUNNER_FUNCTION, runnerType())
            .addStatement("val $VERIFY_FAILURES_PROPERTY = mutableListOf<String>()")
            // Every shot goes through UI-tree capture — a pass-through unless the Gradle plugin
            // enabled it. Wrapping the runner here keeps the component wrappers and both
            // engines' test generators untouched.
            .addStatement(
                "val %L: %T = { name, content -> %L(name) { %M(name, %L, content) } }",
                CAPTURING_RUNNER,
                runnerType(),
                RUNNER_FUNCTION,
                UI_TREE_CAPTURE,
                APP_PACKAGES_PROPERTY,
            )
            // CodeBlocks are added directly, never rendered to String and re-parsed:
            // addCode(String) treats '%' as format specifiers and bypasses MemberName
            // import handling.
            .addCode(
                CodeBlock.builder()
                    .apply { holders.forEach { add(getRunnerFunction(it, wrapperSuffix)) } }
                    .build(),
            )
            .addCode(getAggregateThrow())
            .build()
    }

    private fun runnerType(): LambdaTypeName = LambdaTypeName.get(
        parameters = arrayOf(
            ClassName(KOTLIN_PACKAGE, "String"),
            LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(getComposableAnnotation())),
        ),
        returnType = UNIT,
    )

    private fun getAggregateThrow(): CodeBlock {
        // Verification reports every mismatched component at once instead of failing on
        // the first — a re-record fixes them all in one pass.
        return CodeBlock.builder().apply {
            beginControlFlow("if ($VERIFY_FAILURES_PROPERTY.isNotEmpty())")
            addStatement(
                "throw AssertionError(%S + $VERIFY_FAILURES_PROPERTY.size + %S + " +
                    "$VERIFY_FAILURES_PROPERTY.joinToString(%S) { %S + it })",
                "Arkive: snapshot verification failed for ",
                " component(s):\n",
                "\n",
                "  - ",
            )
            endControlFlow()
        }.build()
    }

    private fun getArkiveClass(): TypeSpec.Builder = TypeSpec.classBuilder(SIMPLE_NAME)
        .addProperty(
            // Whichever engine runs, its verify task sets its own system property on the
            // test JVM — the shooter stays engine-agnostic by keying on either. The same
            // literals are read in the generated test class (testprocessor generators);
            // keep them in sync.
            PropertySpec.builder(IS_VERIFY_RUN_PROPERTY, BOOLEAN)
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    "java.lang.Boolean.getBoolean(%S) || java.lang.Boolean.getBoolean(%S)",
                    "paparazzi.test.verify",
                    "roborazzi.test.verify",
                )
                .build(),
        )
        .addProperty(
            PropertySpec.builder(APP_PACKAGES_PROPERTY, SET.parameterizedBy(STRING))
                .addModifiers(KModifier.PRIVATE)
                .initializer(appPackagesInitializer())
                .build(),
        )

    private fun appPackagesInitializer(): CodeBlock = CodeBlock.builder().apply {
        add("setOf(")
        appPackages.forEachIndexed { index, name ->
            if (index > 0) add(", ")
            add("%S", name)
        }
        add(")")
    }.build()

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
        private const val KOTLIN_PACKAGE = "kotlin"
        private const val IS_VERIFY_RUN_PROPERTY = "isVerifyRun"
        private const val VERIFY_FAILURES_PROPERTY = "verifyFailures"
        private const val CAPTURING_RUNNER = "capturingRunner"
        private const val APP_PACKAGES_PROPERTY = "appPackages"
        private val UI_TREE_CAPTURE = MemberName("com.infinum.arkive.composeutils.uitree", "ArkiveUiTreeCapture")
    }
}
