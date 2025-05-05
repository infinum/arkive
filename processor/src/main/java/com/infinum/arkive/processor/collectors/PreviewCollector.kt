package com.infinum.arkive.processor.collectors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.infinum.arkive.processor.collectors.ArkiveComposableCollector.Companion.TAG_COMPOSABLE
import com.infinum.arkive.processor.models.UiComponentHolder

class PreviewCollector(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) : Collector<UiComponentHolder> {

    @Suppress("LabeledExpression")
    override fun collect(): Set<UiComponentHolder> {
        return resolver.getSymbolsWithAnnotation(COMPOSABLE_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull {
                val previewAnnotation = it.getMainPreviewAnnotation()
                    ?: it.getVariantsAnnotation()
                    ?: return@mapNotNull null

                UiComponentHolder(
                    function = it,
                    functionName = it.simpleName.getShortName(),
                    name = previewAnnotation.getStringArgument("name").orEmpty(),
                    group = previewAnnotation.getStringArgument("group").orEmpty(),
                    skip = false,
                    tags = listOf(TAG_COMPOSABLE),
                    extraMetadata = emptyList(),
                    packageName = it.packageName.asString(),
                    parameters = it.parameters,
                )
            }
            .toSet().also {
                logger.info("Collected ${it.size} @Previews")
            }
    }

    private fun KSFunctionDeclaration.getMainPreviewAnnotation(): KSAnnotation? =
        annotations.firstOrNull { annotation -> annotation.shortName.getShortName() == PREVIEW_ANNOTATION_NAME }

    private fun KSFunctionDeclaration.getVariantsAnnotation(): KSAnnotation? {
        return annotations.firstOrNull { annotation ->
            annotation.shortName.getShortName().startsWith(PREVIEW_ANNOTATION_NAME)
        }
    }

    private fun KSAnnotation.getStringArgument(name: String): String? {
        return arguments
            .firstOrNull { arg ->
                arg.name?.getShortName() == name
            }?.value as? String
    }

    companion object {
        const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
        const val PREVIEW_ANNOTATION_NAME = "Preview"
    }
}
