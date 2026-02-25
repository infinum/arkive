package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector
import com.infinum.arkive.processor.specs.ComposeMetaDataSpec
import com.infinum.arkive.processor.specs.ComposeRunnerSpec
import com.infinum.arkive.processor.specs.ComposeVariantSpec
import com.infinum.arkive.processor.validators.ComposeValidator

class ComposeSubprocessor(
    private val disablePreviewParameters: Boolean,
    private val enableVariants: Boolean,
) : Subprocessor {
    override fun process(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val collector = ArkiveComposableCollector(resolver, logger)
        val validator = ComposeValidator(logger)

        with(validator.validate(collector.collect())) {
            ComposeVariantSpec(codeGenerator, this, logger, disablePreviewParameters, enableVariants).write()
            ComposeRunnerSpec(codeGenerator, this, logger).write()
            ComposeMetaDataSpec(codeGenerator, this).write()
        }
    }
}
