package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.processor.models.ComposeHolder

class ArkiveComposableCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<ComposeHolder> {

    @OptIn(KspExperimental::class)
    override fun collect(): Set<ComposeHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_ARKIVE_COMPOSABLE)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                val arkiveComposable = it.getAnnotationsByType(ArkiveComposable::class).first()
                val name = arkiveComposable.name.ifEmpty { it.simpleName.getShortName() }
                ComposeHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = name,
                    group = arkiveComposable.group,
                    skip = arkiveComposable.skip,
                    tags = arkiveComposable.tags.toList().plus(TAG_COMPOSABLE),
                    extraMetadata = arkiveComposable.extraMetadata.toList(),
                    figmaNodeId = arkiveComposable.designNodeId.ifEmpty { null },
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                    fileName = it.containingFile?.fileName.orEmpty(),
                )
            }
            .toSet().also {
                logger.info("Collected ${it.size} @ArkiveComposable")
            }
    }

    companion object {
        val ANNOTATION_ARKIVE_COMPOSABLE = ArkiveComposable::class.qualifiedName.toString()
        const val TAG_COMPOSABLE = "composable"
    }
}
