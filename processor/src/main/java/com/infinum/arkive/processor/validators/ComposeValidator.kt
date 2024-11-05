package com.infinum.arkive.processor.validators

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.infinum.arkive.processor.models.ComposeHolder

class ComposeValidator : Validator<ComposeHolder> {
    override fun validate(elements: Set<ComposeHolder>) = elements.filter {
        it.parameters.isEmpty() && it.function.isPublic
    }.toSet()

    private val KSFunctionDeclaration.isPublic: Boolean
        get() {
            return when {
                modifiers.isEmpty() -> true
                modifiers.contains(Modifier.PUBLIC) -> true
                else -> false
            }
        }
}