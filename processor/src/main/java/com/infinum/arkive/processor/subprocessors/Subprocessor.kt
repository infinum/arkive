package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver

interface Subprocessor {
    fun process(resolver: Resolver, codeGenerator: CodeGenerator)
}