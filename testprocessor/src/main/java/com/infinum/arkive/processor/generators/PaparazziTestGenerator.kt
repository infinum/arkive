package com.infinum.arkive.processor.generators

import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.JUNIT_PACKAGE
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.RETENTION_SYSTEM_PROPERTY
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.TEST_CLASS_NAME
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.testAnnotation
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Paparazzi/layoutlib test: three test methods that pump every component through the
 * generated shooter in one run. layoutlib renders each snapshot standalone (no window
 * survives between captures), so the single-method shape is memory-safe here — unlike
 * under Robolectric, which is why [RoborazziTestGenerator] has a different shape.
 *
 * Density and font-scale variants are device settings, not Compose overrides: before a
 * shot whose `ArkiveShotConfig` asks for another device, the test switches Paparazzi's
 * render session to it (`unsafeUpdateConfig`), so resources, the configuration and
 * hosted Views see the change too — the same model the Roborazzi test uses.
 */
internal class PaparazziTestGenerator : EngineTestGenerator {

    override fun generate(): TypeSpec {
        return TypeSpec.classBuilder(TEST_CLASS_NAME)
            .addModifiers(KModifier.PUBLIC)
            .addProperty(isVerifyRunProperty())
            .addProperty(retentionProperty())
            .addProperty(baseDeviceProperty())
            .addProperty(currentDeviceProperty())
            .addProperty(paparazziProperty())
            .addProperty(ruleChainProperty())
            .addFunction(deviceForFunction())
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
    // code keys off the same source of truth instead of a parallel flag.
    private fun isVerifyRunProperty(): PropertySpec {
        return PropertySpec.builder(IS_VERIFY_RUN_PROPERTY, BOOLEAN)
            .addModifiers(KModifier.PRIVATE)
            .initializer("java.lang.Boolean.getBoolean(%S)", "paparazzi.test.verify")
            .build()
    }

    // Injected by the Arkive Gradle plugin so verify runs only enforce snapshots whose
    // goldens the retention policy actually kept — a consumer's plain verifyPaparazzi with
    // retention NONE must not fail on golden-less Arkive snapshots.
    private fun retentionProperty(): PropertySpec {
        return PropertySpec.builder(RETENTION_PROPERTY, ClassName("kotlin", "String"))
            .addModifiers(KModifier.PRIVATE)
            .initializer("System.getProperty(%S) ?: %S", RETENTION_SYSTEM_PROPERTY, "NONE")
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
                // useDeviceResolution saves every image at the size it rendered at. By default
                // Paparazzi shrinks any image whose long side exceeds 1000 px, which saved a
                // full screen at xhdpi, xxhdpi and xxxhdpi alike as 562x1000 and broke the
                // PNG size = UI-tree size x density invariant. Roborazzi never downscales.
                "Paparazzi(" +
                    "deviceConfig = $BASE_DEVICE_PROPERTY, " +
                    "renderingMode = com.android.ide.common.rendering.api.SessionParams.RenderingMode.SHRINK, " +
                    "theme = \"android:Theme.Translucent.NoTitleBar\", " +
                    "useDeviceResolution = true" +
                    ")",
            )
            .build()
    }

    // Explicit although it is Paparazzi's own default: deviceFor() scales every variant
    // device from it, so the session's starting device and that base must never drift.
    private fun baseDeviceProperty(): PropertySpec {
        return PropertySpec.builder(BASE_DEVICE_PROPERTY, DEVICE_CONFIG)
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T.NEXUS_5", DEVICE_CONFIG)
            .build()
    }

    // The device the render session uses right now. JUnit builds a fresh instance per test
    // method, and the rule a fresh session on the base device, so it starts there.
    private fun currentDeviceProperty(): PropertySpec {
        return PropertySpec.builder(CURRENT_DEVICE_PROPERTY, DEVICE_CONFIG)
            .addModifiers(KModifier.PRIVATE)
            .mutable()
            .initializer(BASE_DEVICE_PROPERTY)
            .build()
    }

    // A real xxxhdpi phone has more pixels, not less room: the pixel size (and physical dpi)
    // scales with the density, so every density variant keeps the base device's dp size.
    private fun deviceForFunction(): FunSpec {
        return FunSpec.builder(DEVICE_FOR_FUNCTION)
            .addModifiers(KModifier.PRIVATE)
            .addParameter(ParameterSpec.builder("config", SHOT_CONFIG).build())
            .returns(DEVICE_CONFIG)
            .addCode(
                """
                val baseDpi = $BASE_DEVICE_PROPERTY.density.dpiValue
                val dpi = config.densityDpi ?: baseDpi
                val scale = dpi.toDouble() / baseDpi
                return $BASE_DEVICE_PROPERTY.copy(
                    screenWidth = ($BASE_DEVICE_PROPERTY.screenWidth * scale).%M(),
                    screenHeight = ($BASE_DEVICE_PROPERTY.screenHeight * scale).%M(),
                    xdpi = ($BASE_DEVICE_PROPERTY.xdpi * scale).%M(),
                    ydpi = ($BASE_DEVICE_PROPERTY.ydpi * scale).%M(),
                    density = %T.create(dpi),
                    fontScale = config.fontScale ?: $BASE_DEVICE_PROPERTY.fontScale,
                )
                """.trimIndent(),
                ROUND_TO_INT,
                ROUND_TO_INT,
                ROUND_TO_INT,
                ROUND_TO_INT,
                DENSITY,
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
                AnnotationSpec.builder(ClassName(JUNIT_PACKAGE, "Rule"))
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
            .addAnnotation(testAnnotation())
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
                shooter.$shooterFunction { name, config, function ->
                    // Rebuilding the render session is costly, so switch only when the
                    // shot needs a different device than the current one.
                    val device = $DEVICE_FOR_FUNCTION(config)
                    if (device != $CURRENT_DEVICE_PROPERTY) {
                        paparazzi.unsafeUpdateConfig(deviceConfig = device)
                        $CURRENT_DEVICE_PROPERTY = device
                    }
                    paparazzi.snapshot(name = name) { function() }
                }
                """.trimIndent(),
            )
            .build()
    }

    private fun viewTestFunction(): FunSpec {
        val layoutInflaterClass = ClassName("android.view", "LayoutInflater")

        return FunSpec.builder("testAllViewFunctions")
            .addAnnotation(testAnnotation())
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
                    val view = %T.from(paparazzi.context).inflate(viewId, %T.host(paparazzi.context, name))
                    paparazzi.snapshot(view = view, name = name)
                }
                """.trimIndent(),
                layoutInflaterClass,
                UI_TREE_VIEWS,
            )
            .build()
    }

    companion object {
        private const val IS_VERIFY_RUN_PROPERTY = "isVerifyRun"
        private const val RETENTION_PROPERTY = "snapshotRetention"
        private const val BASE_DEVICE_PROPERTY = "baseDevice"
        private const val CURRENT_DEVICE_PROPERTY = "currentDevice"
        private const val DEVICE_FOR_FUNCTION = "deviceFor"

        private val UI_TREE_VIEWS = ClassName("com.infinum.arkive.composeutils.uitree", "ArkiveUiTreeViews")
        private val DEVICE_CONFIG = ClassName("app.cash.paparazzi", "DeviceConfig")
        private val DENSITY = ClassName("com.android.resources", "Density")
        private val SHOT_CONFIG = ClassName("com.infinum.arkive.composeutils", "ArkiveShotConfig")
        private val ROUND_TO_INT = MemberName("kotlin.math", "roundToInt")
    }
}
