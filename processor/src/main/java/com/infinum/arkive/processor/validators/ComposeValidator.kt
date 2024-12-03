package com.infinum.arkive.processor.validators

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.infinum.arkive.processor.models.ComposeHolder

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
        it.skip.not() && it.parameters.isEmpty() && it.function.hasValidScope
    }.toSet()
}
