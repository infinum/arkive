package com.infinum.arkive.processor.repository

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector
import com.infinum.arkive.processor.collectors.ArkiveViewCollector
import com.infinum.arkive.processor.collectors.PreviewCollector
import com.infinum.arkive.processor.models.ArkiveOptions
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.models.ViewHolder
import com.infinum.arkive.processor.validators.ComposeValidator
import com.infinum.arkive.processor.validators.ViewValidator

class ComponentRepository {
    private var composeHolders: Set<ComposeHolder> = emptySet()
    private var viewHolders: Set<ViewHolder> = emptySet()

    fun getComposeHolders(resolver: Resolver, logger: KSPLogger, options: ArkiveOptions): Set<ComposeHolder> {
        if (composeHolders.isEmpty()) {
            val arkiveComposableCollector = ArkiveComposableCollector(resolver, logger)
            val previewCollector = PreviewCollector(resolver, logger)
            val validator = ComposeValidator(logger)

            composeHolders = validator.validate(
                buildSet {
                    addAll(arkiveComposableCollector.collect())
                    if (options.skipPreviews.not()) {
                        addAll(previewCollector.collect())
                    }
                }
            )
        }
        return composeHolders
    }

    fun getViewHolders(resolver: Resolver, logger: KSPLogger, options: ArkiveOptions): Set<ViewHolder> {
        if (viewHolders.isEmpty()) {
            val arkiveViewCollector = ArkiveViewCollector(resolver, logger)
            val validator = ViewValidator(logger)

            viewHolders = validator.validate(
                buildSet {
                    addAll(arkiveViewCollector.collect())
                }
            )
        }
        return viewHolders
    }

    fun clearComposeAndViewHolders() {
        composeHolders = emptySet()
        viewHolders = emptySet()
    }
}