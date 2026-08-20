package com.infinum.arkive.processor.generators

import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.RETENTION_SYSTEM_PROPERTY
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.SNAPSHOT_FILE_PREFIX
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.TEST_CLASS_NAME
import com.infinum.arkive.processor.generators.EngineTestGenerator.Companion.testAnnotation
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec

/**
 * Roborazzi/Robolectric test: ONE parameterized test per snapshot, via
 * `ParameterizedRobolectricTestRunner` fed from the generated shooters (this processor
 * cannot see the components itself — SOURCE retention, different compilation — so the
 * parameters method *collects* them by running the shooters with a recording callback
 * instead of a capturing one).
 *
 * Per-snapshot tests are not a style choice: Robolectric tears down activities and their
 * compositions per test METHOD, so a single method capturing hundreds of full screens
 * accumulates every window until the method ends — GC thrash, then OutOfMemoryError on
 * real apps. One test per snapshot keeps memory flat at one screen's worth.
 */
internal class RoborazziTestGenerator(
    deviceQualifiers: String,
) : EngineTestGenerator {

    // The device everything renders on. Robolectric's own default (a 320x470dp
    // 2010-era phone) clips real screens; captures already shrink to content, so a
    // roomy modern device only sets the ceiling, not the component snapshot sizes.
    private val device = deviceQualifiers.ifBlank { DEFAULT_DEVICE_QUALIFIERS }

    override fun generate(): TypeSpec {
        val graphicsMode = ClassName(ROBOLECTRIC_ANNOTATION_PACKAGE, "GraphicsMode")
        return TypeSpec.classBuilder(TEST_CLASS_NAME)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                    .addMember("%T::class", PARAMETERIZED_RUNNER)
                    .build(),
            )
            .addAnnotation(
                AnnotationSpec.builder(graphicsMode)
                    .addMember("%T.Mode.NATIVE", graphicsMode)
                    .build(),
            )
            .addAnnotation(
                // SDK 35, not 36: Robolectric's android-all jars for 36 are Java 21
                // bytecode, and staying at 35 keeps the whole stack runnable on JDK 17.
                AnnotationSpec.builder(ClassName(ROBOLECTRIC_ANNOTATION_PACKAGE, "Config"))
                    .addMember("sdk = [35], qualifiers = %S", device)
                    .build(),
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(SHOT_NAME, STRING)
                    .addParameter(SHOT_KIND, STRING)
                    .addParameter(SHOT_CONTENT, ANY)
                    .build(),
            )
            .addProperty(constructorProperty(SHOT_NAME, STRING))
            .addProperty(constructorProperty(SHOT_KIND, STRING))
            .addProperty(constructorProperty(SHOT_CONTENT, ANY))
            .addProperty(
                PropertySpec.builder("isVerifyRun", BOOLEAN)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("java.lang.Boolean.getBoolean(%S)", "roborazzi.test.verify")
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("snapshotRetention", STRING)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("System.getProperty(%S) ?: %S", RETENTION_SYSTEM_PROPERTY, "NONE")
                    .build(),
            )
            .addProperty(
                // The ConsumerAdapter's golden directory (absolute), injected by the
                // Arkive Gradle plugin. Recording straight into it with the shared
                // filename prefix keeps grabber/retention/verify engine-agnostic.
                PropertySpec.builder("snapshotsDir", STRING)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("System.getProperty(%S) ?: %S", "arkive.snapshots.dir", "snapshots")
                    .build(),
            )
            .addProperty(composeRuleProperty())
            .addProperty(ruleChainProperty())
            .addFunction(setUpFunction())
            .addFunction(snapshotTestFunction())
            .addType(parametersCompanion())
            .build()
    }

    // A clock-controlled compose host. The naive captureRoboImage(content) waits for the
    // composition to go idle — an infinite animation (spinner, shimmer, while(true) poll)
    // never idles, and the capture spins the frame clock forever at 100% CPU. Owning the
    // clock (autoAdvance = false + advanceTimeBy) renders a deterministic frame instead.
    // The host activity is launched by EXPLICIT class (Robolectric auto-registers
    // undeclared activities) — createComposeRule()'s MAIN/LAUNCHER intent launch fails
    // under Robolectric because test-classpath AAR manifests never merge in unit tests.
    // Roborazzi's activity is translucent, keeping snapshots' alpha channel — like
    // Paparazzi's Theme.Translucent did.
    private fun composeRuleProperty(): PropertySpec {
        return PropertySpec.builder(
            "composeRule",
            ClassName("androidx.compose.ui.test.junit4", "ComposeContentTestRule"),
        )
            .addModifiers(KModifier.PRIVATE)
            .initializer(
                "%M(%T::class.java)",
                MemberName("androidx.compose.ui.test.junit4", "createAndroidComposeRule"),
                ClassName("com.github.takahirom.roborazzi", "RoborazziActivity"),
            )
            .build()
    }

    // Robolectric refuses to launch activities absent from the merged manifest, and
    // test-classpath AAR manifests never merge in unit tests — Roborazzi ships a helper
    // that registers its activity with Robolectric. It must run BEFORE the compose
    // rule's own setup launches the activity, hence the outer rule (a @Before is too late).
    private fun ruleChainProperty(): PropertySpec {
        return PropertySpec.builder(
            "arkiveRule",
            ClassName("org.junit.rules", "RuleChain"),
        )
            .addAnnotation(
                AnnotationSpec.builder(ClassName(EngineTestGenerator.JUNIT_PACKAGE, "Rule"))
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
                                    com.github.takahirom.roborazzi.registerRoborazziActivityToRobolectricIfNeeded()
                                    base.evaluate()
                                }
                            }
                        }
                    )
                    .around(composeRule)
                """.trimIndent(),
            )
            .build()
    }

    private fun constructorProperty(name: String, type: ClassName): PropertySpec =
        PropertySpec.builder(name, type)
            .addModifiers(KModifier.PRIVATE)
            .initializer(name)
            .build()

    // CMP resources bind their Android context through a ContentProvider that Robolectric
    // does not auto-create in library unit tests. Reflection keeps this a no-op for
    // consumers without Compose Multiplatform on the classpath.
    private fun setUpFunction(): FunSpec {
        return FunSpec.builder("arkiveSetUp")
            .addAnnotation(ClassName(EngineTestGenerator.JUNIT_PACKAGE, "Before"))
            .addCode(
                """
                try {
                    @Suppress("UNCHECKED_CAST")
                    val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
                        as Class<android.content.ContentProvider>
                    org.robolectric.Robolectric.setupContentProvider(provider)
                } catch (ignored: Throwable) {
                    // Not a Compose Multiplatform consumer — nothing to initialize.
                }
                """.trimIndent(),
            )
            .build()
    }

    private fun snapshotTestFunction(): FunSpec {
        return FunSpec.builder("snapshot")
            .addAnnotation(testAnnotation())
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .build(),
            )
            .addCode(
                snapshotTestBody(),
                SNAPSHOT_FILE_PREFIX,
                CAPTURE_ROBO_IMAGE,
                ON_ROOT,
                CAPTURE_ROBO_IMAGE,
            )
            .build()
    }

    private fun snapshotTestBody(): String = """
                if ($SHOT_KIND == "none") {
                    println("Arkive: no components to snapshot")
                    return
                }
                if (isVerifyRun) {
                    // Only enforce snapshots whose goldens the retention policy actually
                    // kept: variants exist only under ALL, everything else needs > NONE.
                    val hasGolden = if ($SHOT_KIND == "variant") snapshotRetention == "ALL" else snapshotRetention != "NONE"
                    if (!hasGolden) {
                        println("Arkive: " + $SHOT_NAME + " has no retained golden (snapshotRetention = " + snapshotRetention + "), nothing to verify")
                        return
                    }
                }
                try {
                    val filePath = snapshotsDir + "/" + %S + $SHOT_NAME + ".png"
                    if ($SHOT_KIND == "view") {
                        val viewId = ($SHOT_CONTENT as () -> Int).invoke()
                        val activity = org.robolectric.Robolectric
                            .buildActivity(android.app.Activity::class.java).setup().get()
                        val view = android.view.LayoutInflater.from(activity)
                            .inflate(viewId, android.widget.FrameLayout(activity))
                        activity.setContentView(view)
                        view.%M(filePath = filePath)
                    } else {
                        val content = $SHOT_CONTENT as @androidx.compose.runtime.Composable () -> Unit
                        // Deterministic frame at t=1s: entry animations settle, infinite
                        // animations can't hang the capture (the clock only moves here).
                        composeRule.mainClock.autoAdvance = false
                        composeRule.setContent {
                            content()
                        }
                        composeRule.mainClock.advanceTimeBy(1_000)
                        composeRule.%M().%M(filePath = filePath)
                    }
                } catch (e: AssertionError) {
                    // Verify mode: a golden mismatch fails exactly this component's test.
                    if (isVerifyRun) {
                        throw e
                    }
                    println("Arkive: skipping component " + $SHOT_NAME + ", snapshot failed: " + e.message)
                } catch (e: Throwable) {
                    // Recording stays resilient: a preview that fails to render is logged
                    // and skipped — it has no golden, so verify must not fail on it either.
                    println("Arkive: skipping component " + $SHOT_NAME + ", snapshot failed: " + e.message)
                }
    """.trimIndent()

    // The parameters: every (name, kind, content) triple the shooters know about. The
    // recording callback never renders anything — collection is cheap and safe.
    private fun parametersCompanion(): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunction(
                FunSpec.builder("shots")
                    .addAnnotation(JvmStatic::class)
                    .addAnnotation(
                        AnnotationSpec.builder(
                            PARAMETERIZED_RUNNER.nestedClass("Parameters"),
                        )
                            .addMember("name = %S", "{0}")
                            .build(),
                    )
                    .returns(LIST.parameterizedBy(ARRAY_OF_ANY))
                    .addCode(
                        """
                        val entries = mutableListOf<Array<Any>>()
                        ArkiveComposeShoot().runComposableTests { name, function ->
                            entries.add(arrayOf(name, "base", function))
                        }
                        ArkiveComposeShoot().runComposableVariantTests { name, function ->
                            entries.add(arrayOf(name, "variant", function))
                        }
                        ArkiveViewShoot().runViewTests { name, function ->
                            entries.add(arrayOf(name, "view", function))
                        }
                        if (entries.isEmpty()) {
                            // The parameterized runner rejects an empty parameter list.
                            entries.add(arrayOf("none", "none", {}))
                        }
                        return entries
                        """.trimIndent(),
                    )
                    .build(),
            )
            .build()
    }

    companion object {
        // Pixel-6-class phone. Consumers override per module via arkive { device.set(...) }.
        private const val DEFAULT_DEVICE_QUALIFIERS = "w411dp-h914dp-420dpi"

        private const val ROBOLECTRIC_ANNOTATION_PACKAGE = "org.robolectric.annotation"
        private const val SHOT_NAME = "shotName"
        private const val SHOT_KIND = "shotKind"
        private const val SHOT_CONTENT = "shotContent"

        private val PARAMETERIZED_RUNNER = ClassName("org.robolectric", "ParameterizedRobolectricTestRunner")
        private val CAPTURE_ROBO_IMAGE = MemberName("com.github.takahirom.roborazzi", "captureRoboImage")
        private val ON_ROOT = MemberName("androidx.compose.ui.test", "onRoot")
        private val ARRAY_OF_ANY = ClassName("kotlin", "Array").parameterizedBy(ANY)
    }
}
