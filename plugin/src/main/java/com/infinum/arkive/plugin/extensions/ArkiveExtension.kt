package com.infinum.arkive.plugin.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class ArkiveExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val multiModuleVariant: Property<String> = objects.property(String::class.java)
        .convention("")

    val disablePreviewParameters: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val enableVariants: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    companion object {
        const val DISABLE_PREVIEW_PARAMETERS = "disablePreviewParameters"
        const val ENABLE_VARIANTS = "enableVariants"
    }
}
