package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.repository.ComponentRepository
import com.infinum.arkive.processor.subprocessors.ComposeSubprocessor
import com.infinum.arkive.processor.subprocessors.MetaDataSubprocessor
import com.infinum.arkive.processor.subprocessors.ViewSubprocessor

class ArkiveProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: ArkiveOptions,
    private val disablePreviewParameters: Boolean,
    private val enableVariants: Boolean,
) : SymbolProcessor {
    private var processed = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        ComposeSubprocessor(disablePreviewParameters, enableVariants).process(resolver, codeGenerator, logger)
        val componentRepository = ComponentRepository()

        ComposeSubprocessor(
            componentRepository = componentRepository,
            disablePreviewParameters = disablePreviewParameters,
            enableVariants = enableVariants,
        ).process(resolver, codeGenerator, logger, options)
        ViewSubprocessor(componentRepository = componentRepository).process(resolver, codeGenerator, logger, options)
        MetaDataSubprocessor(componentRepository = componentRepository).process(resolver, codeGenerator, logger, options)

        componentRepository.clearComposeAndViewHolders()
        return emptyList()
    }
}

class ArkiveProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        val skipPreviews: Boolean = environment.options["skipPreviews"]?.toBooleanStrict() ?: false
        val options = ArkiveOptions(skipPreviews)
        val disablePreviewParameters = environment.options[DISABLE_PREVIEW_PARAMETERS]?.toBoolean() ?: false
        val enableVariants = environment.options[ENABLE_VARIANTS]?.toBoolean() ?: false
        return ArkiveProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = options,
            disablePreviewParameters = disablePreviewParameters,
            enableVariants = enableVariants,
        )
    }

    companion object {
        private const val DISABLE_PREVIEW_PARAMETERS = "disablePreviewParameters"
        private const val ENABLE_VARIANTS = "enableVariants"
    }
}
