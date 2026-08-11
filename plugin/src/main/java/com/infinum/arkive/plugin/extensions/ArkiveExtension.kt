package com.infinum.arkive.plugin.extensions

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class ArkiveExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val multiModuleVariant: Property<String> = objects.property(String::class.java)
        .convention("")

    val enablePreviewParameters: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    val enableVariants: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val designFileKey: Property<String> = objects.property(String::class.java)
        .convention("")

    /**
     * When true (default), snapshots are copied out of Paparazzi's `src/test/snapshots`
     * directory instead of moved, so the golden files stay in place for `verifyPaparazzi`.
     */
    val keepSnapshots: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    companion object {
        const val ENABLE_PREVIEW_PARAMETERS = "enablePreviewParameters"
        const val ENABLE_VARIANTS = "enableVariants"
    }
}
