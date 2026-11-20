package com.infinum.arkive.plugin.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

public open class ArkiveExtension @Inject constructor(
    objects: ObjectFactory,
) {
    public val multiModuleVariant: Property<String> = objects.property(String::class.java)
        .convention("")

    public val enablePreviewParameters: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val enableVariants: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    public val designFileKey: Property<String> = objects.property(String::class.java)
        .convention("")

    /**
     * Fully-qualified name of a top-level `@Composable` theme every snapshot renders inside,
     * e.g. `"com.example.ui.theme.AppTheme"`; empty (default) renders previews as declared.
     * The theme takes a trailing content lambda and may live in another module. A Boolean
     * `darkTheme`/`isDarkTheme`/`useDarkTheme`/`isDark` parameter, if present, is driven
     * by the light/dark `uiMode` variants.
     */
    public val previewTheme: Property<String> = objects.property(String::class.java)
        .convention("")

    /**
     * Named skins, each a theme with the same contract as [previewTheme], in declaration
     * order: `previewThemes.put("red", "com.example.RedTheme")`. The first is the default
     * every snapshot renders in; with two or more, every skin adds `skin_<name>-light|dark` snapshots.
     * Names are letters and digits only. Replaces [previewTheme] — set one or the other.
     * A skin read from global state (preferences, a singleton) must be set by its wrapper
     * during composition: every snapshot runs inside some skin, so none leaks into another.
     */
    public val previewThemes: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
        .convention(emptyMap())

    /**
     * Which snapshots remain in the golden directory after the showcase consumes them:
     * [SnapshotRetention.NONE] (default), [SnapshotRetention.BASE] for base-only golden
     * testing, or [SnapshotRetention.ALL].
     */
    public val snapshotRetention: Property<SnapshotRetention> = objects.property(SnapshotRetention::class.java)
        .convention(SnapshotRetention.NONE)

    /**
     * Writes a UI tree (hierarchy, dp bounds, semantics, text style, composable names and
     * source lines) next to every snapshot, for agents and the showcase's inspector.
     * Informational only — never a golden, never verified. Applies to both engines; toggling
     * it never re-runs KSP.
     */
    public val uiTree: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /** Selector for [engine]: the Roborazzi engine (JDK 17+, renders CMP resources). */
    @Suppress("PropertyName", "VariableNaming")
    public val Roborazzi: RoborazziSelector = RoborazziSelector

    /** Selector for [engine]: the Paparazzi engine (requires a JDK 21+ Gradle daemon). */
    @Suppress("PropertyName", "VariableNaming")
    public val Paparazzi: PaparazziSelector = PaparazziSelector

    internal val roborazziOptions: RoborazziEngineOptions =
        objects.newInstance(RoborazziEngineOptions::class.java)

    // Wired by ArkivePlugin before the consumer's script body runs; selection must reach
    // the plugin the moment the script makes it (the engine's Gradle plugin has to be
    // applied while AGP's variant API is still open).
    internal var onEngineSelected: ((String) -> Unit) = {}

    /**
     * Selects the Roborazzi engine and configures it — selection and engine-scoped
     * options are one call, so options for an engine this module doesn't run are
     * unrepresentable. Selecting an engine is mandatory: a module that never calls
     * `engine(...)` (and isn't covered by the `arkive.engine` property) fails the build.
     */
    @Suppress("UnusedParameter")
    public fun engine(selector: RoborazziSelector, action: Action<RoborazziEngineOptions>) {
        action.execute(roborazziOptions)
        onEngineSelected(ENGINE_ROBORAZZI)
    }

    /** Selects the Roborazzi engine with default options. */
    @Suppress("UnusedParameter")
    public fun engine(selector: RoborazziSelector) {
        onEngineSelected(ENGINE_ROBORAZZI)
    }

    /** Selects the Paparazzi engine. Requires a JDK 21+ Gradle daemon. */
    @Suppress("UnusedParameter")
    public fun engine(selector: PaparazziSelector) {
        onEngineSelected(ENGINE_PAPARAZZI)
    }

    internal companion object {
        internal const val ENABLE_PREVIEW_PARAMETERS = "enablePreviewParameters"
        internal const val ENABLE_VARIANTS = "enableVariants"
        internal const val DEVICE = "arkive.device"
        internal const val TEST_APPLICATION = "arkive.testApplication"
        internal const val PREVIEW_THEME = "arkive.previewTheme"
        internal const val PREVIEW_THEMES = "arkive.previewThemes"
        internal const val ENGINE_ROBORAZZI = "roborazzi"
        internal const val ENGINE_PAPARAZZI = "paparazzi"
    }
}

/** Type-safe selector token for `engine(Roborazzi) { ... }`. */
public object RoborazziSelector

/** Type-safe selector token for `engine(Paparazzi)`. */
public object PaparazziSelector

/** Engine-scoped options: everything here only makes sense on the Roborazzi engine. */
public open class RoborazziEngineOptions @Inject constructor(
    objects: ObjectFactory,
) {
    /**
     * The device snapshots render on, as Robolectric qualifiers. Empty (default) uses
     * Arkive's built-in device (a Pixel-6-class phone, `w411dp-h914dp-420dpi`).
     * Examples: a 10" tablet `w1280dp-h800dp-mdpi`, a desktop-like canvas
     * `w1920dp-h1080dp-mdpi`. Changing it regenerates the test and re-records —
     * goldens recorded on another device won't match.
     */
    public val device: Property<String> = objects.property(String::class.java)
        .convention("")

    /**
     * Fully-qualified name of the `Application` Robolectric starts for Arkive's snapshot
     * test, instead of the one in the manifest. Set it when the app's Application can't
     * start in a unit test (DI graphs, Firebase, SDK init) — typically a plain subclass in
     * `src/test` that initializes only what previews need. Empty (default) keeps
     * Robolectric's choice. Applies to Arkive's generated test only.
     */
    public val testApplication: Property<String> = objects.property(String::class.java)
        .convention("")
}
