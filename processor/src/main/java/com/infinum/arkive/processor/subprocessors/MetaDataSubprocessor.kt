package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.repository.ComponentRepository
import com.infinum.arkive.processor.specs.MetaDataSpec

class MetaDataSubprocessor(
    private val componentRepository: ComponentRepository,
) : Subprocessor {
    override fun process(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger, options: ArkiveOptions) {
        val holders = buildSet {
            addAll(componentRepository.getComposeHolders(resolver, logger, options))
            addAll(componentRepository.getViewHolders(resolver, logger, options))
        }

        MetaDataSpec(codeGenerator, holders).write()
    }
}