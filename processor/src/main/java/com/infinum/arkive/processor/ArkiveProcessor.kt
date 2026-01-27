package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.infinum.arkive.processor.subprocessors.ComposeSubprocessor

class ArkiveProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val disablePreviewParameters: Boolean,
) : SymbolProcessor {
    private var processed = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        ComposeSubprocessor(disablePreviewParameters).process(resolver, codeGenerator, logger)
        return emptyList()
    }
}

class ArkiveProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        val disablePreviewParameters = environment.options[DISABLE_PREVIEW_PARAMETERS]?.toBoolean() ?: false
        return ArkiveProcessor(environment.codeGenerator, environment.logger, disablePreviewParameters)
    }

    companion object {
        private const val DISABLE_PREVIEW_PARAMETERS = "disablePreviewParameters"
    }
}
