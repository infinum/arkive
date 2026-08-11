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
     * Which snapshots remain in Paparazzi's `src/test/snapshots` golden directory after
     * the showcase consumes them: [SnapshotRetention.NONE] (default), [SnapshotRetention.BASE]
     * for base-only golden testing, or [SnapshotRetention.ALL].
     */
    val snapshotRetention: Property<SnapshotRetention> = objects.property(SnapshotRetention::class.java)
        .convention(SnapshotRetention.NONE)

    companion object {
        const val ENABLE_PREVIEW_PARAMETERS = "enablePreviewParameters"
        const val ENABLE_VARIANTS = "enableVariants"
    }
}
