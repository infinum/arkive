package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

// ArkivePlugin.GENERATED_TEST_CLASS hardcodes "$PACKAGE_NAME.$FILE_NAME" for the verify
// test filter — keep the three in sync.
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
            .addProperty(isVerifyRunProperty())
            .addProperty(retentionProperty())
            .addProperty(paparazziProperty())
            .addProperty(ruleChainProperty())
            .addFunction(
                composableTestFunction(
                    "testAllComposableFunctions",
                    "runComposableTests",
                    // Base goldens exist under BASE and ALL retention.
                    verifySkipCondition = "$RETENTION_PROPERTY == \"NONE\"",
                ),
            )
            .addFunction(
                composableTestFunction(
                    "testAllComposableVariants",
                    "runComposableVariantTests",
                    // Variant goldens only exist under ALL retention.
                    verifySkipCondition = "$RETENTION_PROPERTY != \"ALL\"",
                ),
            )
            .addFunction(viewTestFunction())
            .build()
    }

    // Paparazzi flips verify mode via this system property on the test JVM; the generated
    // code keys off the same source of truth instead of a parallel flag. The same literal
    // is read in ComposeRunnerSpec's generated shooter — keep the two in sync.
    private fun isVerifyRunProperty(): PropertySpec {
        return PropertySpec.builder(IS_VERIFY_RUN_PROPERTY, BOOLEAN)
            .addModifiers(KModifier.PRIVATE)
            .initializer("java.lang.Boolean.getBoolean(%S)", "paparazzi.test.verify")
            .build()
    }

    // Injected by the Arkive Gradle plugin (RetentionArgumentProvider in :plugin defines
    // the property name — keep in sync) so verify runs only enforce snapshots whose
    // goldens the retention policy actually kept — a consumer's plain verifyPaparazzi with
    // retention NONE must not fail on golden-less Arkive snapshots.
    private fun retentionProperty(): PropertySpec {
        return PropertySpec.builder(RETENTION_PROPERTY, STRING)
            .addModifiers(KModifier.PRIVATE)
            .initializer("System.getProperty(%S) ?: %S", "arkive.snapshot.retention", "NONE")
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
        // swallowed them. In record mode the outer rule absorbs that, so one broken preview
        // (already logged and skipped) never fails the snapshot run. In verify mode failures
        // are the whole point — they propagate and fail the build.
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
                                        if ($IS_VERIFY_RUN_PROPERTY) {
                                            throw e
                                        }
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
    private fun composableTestFunction(
        testName: String,
        shooterFunction: String,
        verifySkipCondition: String,
    ): FunSpec {
        return FunSpec.builder(testName)
            .addAnnotation(getTestAnnotation())
            .addCode(
                """
                if ($IS_VERIFY_RUN_PROPERTY) {
                    // A typo'd or unreachable retention value must not silently verify nothing.
                    require($RETENTION_PROPERTY in listOf("NONE", "BASE", "ALL")) {
                        "Arkive: unrecognized snapshotRetention '" + $RETENTION_PROPERTY + "' — expected NONE, BASE, or ALL"
                    }
                    if ($verifySkipCondition) {
                        println("Arkive: $testName has no retained goldens (snapshotRetention = " + $RETENTION_PROPERTY + "), nothing to verify")
                        return
                    }
                }
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
                if ($IS_VERIFY_RUN_PROPERTY) {
                    require($RETENTION_PROPERTY in listOf("NONE", "BASE", "ALL")) {
                        "Arkive: unrecognized snapshotRetention '" + $RETENTION_PROPERTY + "' — expected NONE, BASE, or ALL"
                    }
                    if ($RETENTION_PROPERTY == "NONE") {
                        println("Arkive: testAllViewFunctions has no retained goldens (snapshotRetention = NONE), nothing to verify")
                        return
                    }
                }
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

    companion object {
        private const val IS_VERIFY_RUN_PROPERTY = "isVerifyRun"
        private const val RETENTION_PROPERTY = "snapshotRetention"
    }
}

class ArkiveTestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor = ArkiveTestProcessor(environment.codeGenerator, environment.logger)
}
