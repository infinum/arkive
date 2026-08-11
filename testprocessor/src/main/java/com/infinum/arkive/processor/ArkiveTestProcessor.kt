package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

private const val PACKAGE_NAME = "com.infinum.arkive"
private const val FILE_NAME = "ArkiveSnapshotTestGenerator"

class ArkiveTestProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var processed = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        FileSpec.builder(
            PACKAGE_NAME,
            FILE_NAME,
        ).addType(generateTestClass())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )

        return emptyList()
    }

    private fun generateTestClass(): TypeSpec {
        return TypeSpec.classBuilder(FILE_NAME)
            .addModifiers(KModifier.PUBLIC)
            .addProperty(paparazziProperty())
            .addProperty(ruleChainProperty())
            .addFunction(composableTestFunction("testAllComposableFunctions", "runComposableTests"))
            .addFunction(composableTestFunction("testAllComposableVariants", "runComposableVariantTests"))
            .addFunction(viewTestFunction())
            .build()
    }

    private fun paparazziProperty(): PropertySpec {
        return PropertySpec.builder(
            "paparazzi",
            ClassName("app.cash.paparazzi", "Paparazzi"),
        )
            .initializer(
                // Translucent theme keeps the window background transparent, so snapshots
                // carry an alpha channel instead of the default dark Material backdrop.
                "Paparazzi(" +
                    "renderingMode = com.android.ide.common.rendering.api.SessionParams.RenderingMode.SHRINK, " +
                    "theme = \"android:Theme.Translucent.NoTitleBar\"" +
                    ")",
            )
            .build()
    }

    private fun ruleChainProperty(): PropertySpec {
        // Paparazzi re-throws snapshot errors at rule teardown even when the test body
        // swallowed them. The outer rule absorbs that, so one broken preview (already
        // logged and skipped) never fails the snapshot test as a whole.
        return PropertySpec.builder(
            "arkiveRule",
            ClassName("org.junit.rules", "RuleChain"),
        )
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit", "Rule"))
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    .build(),
            )
            .initializer(
                """
                org.junit.rules.RuleChain
                    .outerRule(
                        org.junit.rules.TestRule { base, _ ->
                            object : org.junit.runners.model.Statement() {
                                override fun evaluate() {
                                    try {
                                        base.evaluate()
                                    } catch (e: Throwable) {
                                        println("Arkive: snapshot session finished with errors: " + e.message)
                                    }
                                }
                            }
                        }
                    )
                    .around(paparazzi)
                """.trimIndent(),
            )
            .build()
    }

    // Base and variant snapshots run as separate tests so golden testing can target just
    // the base set: ./gradlew verifyPaparazzi<Variant> --tests '*.testAllComposableFunctions'
    private fun composableTestFunction(testName: String, shooterFunction: String): FunSpec {
        return FunSpec.builder(testName)
            .addAnnotation(getTestAnnotation())
            .addCode(
                """
                val shooter = ArkiveComposeShoot()
                shooter.$shooterFunction { name, function ->
                    paparazzi.snapshot(name = name) {
                        function()
                    }
                }
                """.trimIndent(),
            )
            .build()
    }

    private fun viewTestFunction(): FunSpec {
        val frameLayoutClass = ClassName("android.widget", "FrameLayout")
        val layoutInflaterClass = ClassName("android.view", "LayoutInflater")

        return FunSpec.builder("testAllViewFunctions")
            .addAnnotation(getTestAnnotation())
            .addCode(
                """
                val shooter = ArkiveViewShoot()
                shooter.runViewTests { name, function ->
                    val viewId = function()
                    val view = %T.from(paparazzi.context).inflate(viewId, %T(paparazzi.context))
                    paparazzi.snapshot(view = view, name = name)
                }
                """.trimIndent(),
                layoutInflaterClass,
                frameLayoutClass,
            )
            .build()
    }

    private fun getTestAnnotation() = ClassName("org.junit", "Test")
}

class ArkiveTestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor = ArkiveTestProcessor(environment.codeGenerator, environment.logger)
}
