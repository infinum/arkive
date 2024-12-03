package com.infinum.arkive.processor.specs

import com.squareup.kotlinpoet.FileSpec

interface Spec {
    fun write()
    fun getFileSpec(): FileSpec.Builder
}
