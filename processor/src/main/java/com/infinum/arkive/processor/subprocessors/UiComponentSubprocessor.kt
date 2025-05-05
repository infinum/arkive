package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector
import com.infinum.arkive.processor.collectors.ArkiveViewCollector
import com.infinum.arkive.processor.collectors.PreviewCollector
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.specs.UiComponentMetaDataSpec
import com.infinum.arkive.processor.specs.UiComponentSpec
import com.infinum.arkive.processor.validators.UiComponentValidator

class UiComponentSubprocessor : Subprocessor {
    override fun process(
        resolver: Resolver,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
        options: ArkiveOptions,
    ) {
        val arkiveComposableCollector = ArkiveComposableCollector(resolver, logger)
        val arkiveViewCollector = ArkiveViewCollector(resolver, logger)
        val previewCollector = PreviewCollector(resolver, logger)
        val validator = UiComponentValidator(logger)

        val uiComponentHolders = buildSet {
            addAll(arkiveComposableCollector.collect())
            addAll(arkiveViewCollector.collect())
            if (options.skipPreviews.not()) {
                addAll(previewCollector.collect())
            }
        }

        with(validator.validate(uiComponentHolders)) {
            UiComponentSpec(codeGenerator, this, logger).write()
            UiComponentMetaDataSpec(codeGenerator, this).write()
        }
    }
}
