package com.infinum.arkive.processor.models

internal data class ArkiveOptions(
    val skipPreviews: Boolean,
    // Raw theme args, resolved by PreviewThemeResolver: one fqn, or name=fqn;name=fqn skins.
    val previewTheme: String = "",
    val previewThemes: String = "",
)
