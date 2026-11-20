package com.infinum.arkive.composeutils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Renders [component] as if the system were in light or dark mode: `isSystemInDarkTheme()`
 * follows [LocalConfiguration]'s night bits, and `-night` resources resolve through a
 * context (and, on Compose 1.9+, `LocalResources`) built from the same configuration.
 */
@Composable
public fun UiModeVariant(
    isDark: Boolean,
    component: @Composable () -> Unit,
) {
    val baseConfiguration = LocalConfiguration.current
    val baseContext = LocalContext.current
    val configuration = remember(baseConfiguration, isDark) {
        Configuration(baseConfiguration).apply {
            val nightBits = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBits
        }
    }
    val context = remember(baseContext, configuration) { baseContext.withConfiguration(configuration) }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides context,
    ) {
        val resources = localResources
        if (resources == null) {
            component()
        } else {
            CompositionLocalProvider(resources provides context.resources, content = component)
        }
    }
}

// A wrapper rather than the configuration context itself, so Activity lookups that walk
// the baseContext chain keep working. Falls back to the original context where the
// rendering backend can't build a configuration context (e.g. layoutlib).
private fun Context.withConfiguration(configuration: Configuration): Context {
    val configured = runCatching { createConfigurationContext(configuration) }.getOrNull() ?: return this
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = configured.resources
    }
}

// Compose 1.9+ resolves resources through LocalResources, which this module's Compose
// floor predates — looked up reflectively so it's provided whenever the consumer has it.
@Suppress("UNCHECKED_CAST")
private val localResources: ProvidableCompositionLocal<Resources>? = runCatching {
    Class.forName("androidx.compose.ui.platform.AndroidCompositionLocals_androidKt")
        .getMethod("getLocalResources")
        .invoke(null) as ProvidableCompositionLocal<Resources>
}.getOrNull()

@Composable
private fun UiModeSample() {
    val isDark = isSystemInDarkTheme()
    Text(
        text = if (isDark) "Dark" else "Light",
        color = if (isDark) Color.White else Color.Black,
        modifier = Modifier
            .background(if (isDark) Color.Black else Color.White)
            .padding(16.dp),
    )
}

@Preview
@Composable
private fun PreviewLightUiModeVariant() {
    UiModeVariant(isDark = false) { UiModeSample() }
}

@Preview
@Composable
private fun PreviewDarkUiModeVariant() {
    UiModeVariant(isDark = true) { UiModeSample() }
}
