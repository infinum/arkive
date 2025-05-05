package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.annotations.ArkiveView
import com.infinum.arkive.processor.models.ComposeHolder

class ArkiveViewCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<ComposeHolder> {

    @OptIn(KspExperimental::class)
    override fun collect(): Set<ComposeHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_ARKIVE_VIEW)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                val arkiveView = it.getAnnotationsByType(ArkiveView::class).first()
                val name = arkiveView.name.ifEmpty { it.simpleName.getShortName() }
                ComposeHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = name,
                    group = arkiveView.group,
                    skip = arkiveView.skip,
                    tags = arkiveView.tags.toList().plus(TAG_VIEW),
                    extraMetadata = arkiveView.extraMetadata.toList(),
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                )
            }
            .toSet().also {
                logger.info("Collected ${it.size} @ArkiveView")
            }
    }

    companion object {
        val ANNOTATION_ARKIVE_VIEW = ArkiveView::class.qualifiedName.toString()
        const val TAG_VIEW = "view"
    }
}
