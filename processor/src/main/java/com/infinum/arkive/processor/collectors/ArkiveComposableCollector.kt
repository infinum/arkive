package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.processor.models.ComposeHolder

class ArkiveComposableCollector(
    private val resolver: Resolver,
) : Collector<ComposeHolder> {

    @OptIn(KspExperimental::class)
    override fun collect(): Set<ComposeHolder> {
        return resolver.getSymbolsWithAnnotation(ANNOTATION_ARKIVE_COMPOSABLE)
            .filterIsInstance<KSFunctionDeclaration>()
            .map {
                val arkiveComposable = it.getAnnotationsByType(ArkiveComposable::class).first()
                val name =
                    arkiveComposable.name.ifEmpty { it.simpleName.getShortName() }
                ComposeHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = name,
                    group = arkiveComposable.group,
                    skip = arkiveComposable.skip,
                    tags = arkiveComposable.tags.toList(),
                    extraMetadata = arkiveComposable.extraMetadata.toList(),
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                )
            }
            .toSet()
    }

    companion object {
        val ANNOTATION_ARKIVE_COMPOSABLE = ArkiveComposable::class.qualifiedName.toString()
    }
}
