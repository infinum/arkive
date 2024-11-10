package com.infinum.arkive.processor.specs

import com.squareup.kotlinpoet.FileSpec

interface Spec {
    fun write()
}

interface KotlinSpec : Spec {
    fun getFileSpec(): FileSpec.Builder
}
