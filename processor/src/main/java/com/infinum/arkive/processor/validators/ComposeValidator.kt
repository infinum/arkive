package com.infinum.arkive.processor.validators

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.specs.ComposeVariantSpec.Companion.PREVIEW_PARAMETER_ANNOTATION_NAME

class ComposeValidator(
    private val logger: KSPLogger,
) : Validator<ComposeHolder> {
    private val KSFunctionDeclaration.isPublic: Boolean
        get() {
            return when {
                modifiers.isEmpty() -> true
                modifiers.contains(Modifier.PUBLIC) -> true
                else -> false
            }
        }
    private val KSFunctionDeclaration.isInternal: Boolean
        get() = modifiers.contains(Modifier.INTERNAL)

    private val KSFunctionDeclaration.hasValidScope: Boolean
        get() = isPublic || isInternal

    override fun validate(elements: Set<ComposeHolder>) = elements.filter {
        it.skip.not() && verifyParameters(it) && it.function.hasValidScope
    }.toSet()
        .also {
            logger.info("Validated composable to only ${it.size}")
        }

    /**
     * Check if there is no parameters or only one parameter with annotation `@PreviewParameter`
     */
    private fun verifyParameters(composeHolder: ComposeHolder) = composeHolder.parameters.isEmpty() ||
        (composeHolder.parameters.size == 1 &&
            composeHolder.parameters[0].annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAMETER_ANNOTATION_NAME
            } != null
        )
}
