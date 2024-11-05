package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.infinum.arkive.processor.subprocessors.ComposeSubprocessor

lateinit var logger: KSPLogger
var packagesPath: String? = null

class ArkiveProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private var processed = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        ComposeSubprocessor().process(resolver, codeGenerator)
        return emptyList()
    }
}

class ArkiveProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        logger = environment.logger
        //packagesPath = environment.options["packageFilePath"]
        return ArkiveProcessor(environment.codeGenerator)
    }
}
