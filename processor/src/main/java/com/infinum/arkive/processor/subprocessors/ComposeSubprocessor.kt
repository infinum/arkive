package com.infinum.arkive.processor.subprocessors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.repository.ComponentRepository
import com.infinum.arkive.processor.shared.ModulePackages
import com.infinum.arkive.processor.specs.ComposeRunnerSpec
import com.infinum.arkive.processor.specs.ComposeVariantSpec

class ComposeSubprocessor(
    private val componentRepository: ComponentRepository,
    private val enablePreviewParameters: Boolean,
    private val enableVariants: Boolean,
) : Subprocessor {
    override fun process(
        resolver: Resolver,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
        options: ArkiveOptions,
    ) {
        val composeHolders = componentRepository.getComposeHolders(resolver, logger, options)
        val appPackages = ModulePackages.from(resolver.getAllFiles().map { it.packageName.asString() })
        ComposeVariantSpec(codeGenerator, composeHolders, enablePreviewParameters, enableVariants).write()
        ComposeRunnerSpec(codeGenerator, composeHolders, appPackages, logger).write()
    }
}
