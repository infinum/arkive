package com.infinum.arkive.plugin.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class ArkiveExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val variant: Property<String> = objects.property(String::class.java)
        .convention("")
}