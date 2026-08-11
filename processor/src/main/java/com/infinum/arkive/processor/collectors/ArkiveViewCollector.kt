package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.annotations.ArkiveView
import com.infinum.arkive.processor.models.ViewHolder

class ArkiveViewCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<ViewHolder> {

    @OptIn(KspExperimental::class)
    override fun collect(): Set<ViewHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_ARKIVE_VIEW)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                val arkiveView = it.getAnnotationsByType(ArkiveView::class).first()
                val name = arkiveView.name.ifEmpty { it.simpleName.getShortName() }
                ViewHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = name,
                    group = arkiveView.group,
                    skip = arkiveView.skip,
                    tags = arkiveView.tags.toList().plus(TAG_VIEW),
                    extraMetadata = arkiveView.extraMetadata.toList(),
                    figmaNodeId = arkiveView.designNodeId.ifEmpty { null },
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                    fileName = it.containingFile?.fileName.orEmpty()
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
