package com.infinum.arkive.processor.models

import com.squareup.kotlinpoet.MemberName

/**
 * A theme wrapper generated snapshots render inside — the module's single preview theme,
 * or one of its named skins. Called with its fixed [arguments] (verbatim `name = value`
 * code, e.g. `brand = com.example.Brand.RED`) and a trailing content lambda;
 * [darkThemeParameter] is the Boolean dark-mode parameter when the theme has one, passed
 * explicitly by the light/dark variants.
 */
internal data class PreviewTheme(
    val name: String,
    val member: MemberName,
    val arguments: List<String>,
    val darkThemeParameter: String?,
    // A dark-mode parameter without a default must be passed everywhere, not just in uiMode variants.
    val darkThemeRequired: Boolean,
)
