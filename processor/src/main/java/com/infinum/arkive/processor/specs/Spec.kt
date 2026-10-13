package com.infinum.arkive.processor.specs

import com.squareup.kotlinpoet.FileSpec

internal interface Spec {
    fun write()
}

internal interface KotlinSpec : Spec {
    fun getFileSpec(): FileSpec.Builder
}
