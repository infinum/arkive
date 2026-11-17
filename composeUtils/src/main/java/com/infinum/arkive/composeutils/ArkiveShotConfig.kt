package com.infinum.arkive.composeutils

import kotlin.math.roundToInt

/**
 * The device settings one snapshot asks its engine for. Density and font-scale variants are
 * rendered by the engine's own device configuration, not by Compose overrides: an override
 * keeps the device's pixel window, so a density change shrinks or stretches the dp space the
 * content lays out in, and resources, `LocalConfiguration` and hosted Views never see it.
 *
 * The generated shooter passes one with every shot; base snapshots and variants that stay
 * plain Compose overrides (layout direction, preview parameters) pass [Default]. Each engine's
 * generated test applies it its own way. Called by Arkive's generated code; not meant for
 * direct use.
 *
 * @property density screen density as a scale factor (1.0 = mdpi), or null for the device's own.
 * @property fontScale system font scale, or null for the device's own.
 */
public data class ArkiveShotConfig(
    public val density: Float? = null,
    public val fontScale: Float? = null,
) {
    /** [density] in dpi, the unit Android resource qualifiers use; null when [density] is. */
    public val densityDpi: Int?
        get() = density?.let { (it * DPI_PER_DENSITY).roundToInt() }

    public companion object {
        private const val DPI_PER_DENSITY = 160f

        /** No overrides: the device exactly as the engine is configured. */
        public val Default: ArkiveShotConfig = ArkiveShotConfig()
    }
}
