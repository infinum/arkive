package com.infinum.arkive.plugin.extensions

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
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
     * Which snapshots remain in the golden directory after the showcase consumes them:
     * [SnapshotRetention.NONE] (default), [SnapshotRetention.BASE] for base-only golden
     * testing, or [SnapshotRetention.ALL].
     */
    public val snapshotRetention: Property<SnapshotRetention> = objects.property(SnapshotRetention::class.java)
        .convention(SnapshotRetention.NONE)

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
}
