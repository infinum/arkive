package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector
import com.infinum.arkive.processor.specs.ComposeMetaDataSpec
import com.infinum.arkive.processor.specs.ComposeSpec
import com.infinum.arkive.processor.validators.ComposeValidator

class ComposeSubprocessor : Subprocessor {
    override fun process(resolver: Resolver, codeGenerator: CodeGenerator) {
        val collector = ArkiveComposableCollector(resolver)
        val validator = ComposeValidator()

        with(validator.validate(collector.collect())) {
            ComposeSpec(codeGenerator, this).write()
            ComposeMetaDataSpec(codeGenerator, this).write()
        }
    }
}
