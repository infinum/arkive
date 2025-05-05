package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.processor.models.UiComponentHolder

class ArkiveComposableCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<UiComponentHolder> {

    @OptIn(KspExperimental::class)
    override fun collect(): Set<UiComponentHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_ARKIVE_COMPOSABLE)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                val arkiveComposable = it.getAnnotationsByType(ArkiveComposable::class).first()
                val name = arkiveComposable.name.ifEmpty { it.simpleName.getShortName() }
                UiComponentHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = name,
                    group = arkiveComposable.group,
                    skip = arkiveComposable.skip,
                    tags = arkiveComposable.tags.toList().plus(TAG_COMPOSABLE),
                    extraMetadata = arkiveComposable.extraMetadata.toList(),
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
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
