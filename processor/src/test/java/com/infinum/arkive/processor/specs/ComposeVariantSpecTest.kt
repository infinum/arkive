package com.infinum.arkive.processor.specs

import com.infinum.arkive.processor.models.ComposeHolder
import java.lang.reflect.Proxy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeVariantSpecTest {

    private val holder = ComposeHolder(
        name = "Card",
        functionName = "Card",
        packageName = "com.app.ui",
        skip = false,
        group = "",
        tags = emptyList(),
        extraMetadata = emptyList(),
        figmaNodeId = null,
        function = unused(),
        parameters = emptyList(),
        fileName = "Card.kt",
    )

    // Whitespace-normalized: KotlinPoet wraps long lines at spaces.
    private val source = ComposeVariantSpec(
        codeGenerator = unused(),
        holders = setOf(holder),
        enablePreviewParameters = true,
        enableVariants = true,
    ).fileSpec().toString().replace(Regex("\\s+"), " ")

    @Test
    fun baseShotsUseTheDeviceAsConfigured() {
        assertTrue(source, source.contains("runner(\"com-app-ui-card\", ArkiveShotConfig.Default) { Card() }"))
    }

    @Test
    fun densityVariantsAskTheEngineInsteadOfOverridingCompose() {
        assertTrue(
            source,
            source.contains("runner(\"com-app-ui-card_density_xxxhdpi\", ArkiveShotConfig(density = 4.0f)) { Card() }"),
        )
        assertTrue(
            source,
            source.contains("runner(\"com-app-ui-card_density_ldpi\", ArkiveShotConfig(density = 0.75f)) { Card() }"),
        )
        assertFalse(source, source.contains("DensityVariant"))
    }

    @Test
    fun fontVariantsAskTheEngineInsteadOfOverridingCompose() {
        assertTrue(
            source,
            source.contains("runner(\"com-app-ui-card_font_1.5\", ArkiveShotConfig(fontScale = 1.5f)) { Card() }"),
        )
        assertFalse(source, source.contains("FontVariant"))
    }

    @Test
    fun layoutDirectionStaysAComposeOverride() {
        assertTrue(
            source,
            source.contains(
                "runner(\"com-app-ui-card_layoutDirection_RTL\", ArkiveShotConfig.Default) " +
                    "{ LayoutDirectionVariant(isLtr = false) { Card() } }",
            ),
        )
    }

    private inline fun <reified T : Any> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            error("unexpected call to ${method.name}")
        } as T
}
