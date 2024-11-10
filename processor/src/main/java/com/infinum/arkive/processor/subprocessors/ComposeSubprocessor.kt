package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.collectors.ComposeCollector
import com.infinum.arkive.processor.specs.ComposeSpec
import com.infinum.arkive.processor.validators.ComposeValidator

class ComposeSubprocessor : Subprocessor {
    override fun process(resolver: Resolver, codeGenerator: CodeGenerator) {
        val collector = ComposeCollector(resolver)
        val validator = ComposeValidator()

        with(validator.validate(collector.collect())) {
            ComposeSpec(codeGenerator, this).write()
        }
    }
}
