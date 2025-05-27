package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.repository.ComponentRepository
import com.infinum.arkive.processor.specs.ViewSpec

class ViewSubprocessor : Subprocessor {
    override fun process(
        resolver: Resolver,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
        options: ArkiveOptions,
    ) {
        val viewHolders = ComponentRepository.getViewHolders(resolver, logger, options)
        ViewSpec(codeGenerator, viewHolders, logger).write()
    }
}
