package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.processor.models.ComposeHolder

class ComposeCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<ComposeHolder> {

    override fun collect(): Set<ComposeHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_PREVIEW)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                ComposeHolder(
                    function = it,
                    name = it.simpleName.getShortName(),
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                )
            }
            .toSet()
    }

    companion object {
        const val ANNOTATION_PREVIEW = "androidx.compose.ui.tooling.preview.Preview"
    }
}
