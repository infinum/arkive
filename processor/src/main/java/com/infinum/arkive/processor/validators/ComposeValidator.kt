package com.infinum.arkive.processor.validators

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.specs.ComposeVariantSpec.Companion.PREVIEW_PARAMETER_ANNOTATION_NAMES

class ComposeValidator(
    private val logger: KSPLogger,
) : Validator<ComposeHolder> {
    private val KSDeclaration.hasValidScope: Boolean
        get() = !modifiers.contains(Modifier.PRIVATE) && !modifiers.contains(Modifier.PROTECTED)

    /**
     * Drops previews Arkive cannot generate code for. A problem on an explicitly
     * `@ArkiveComposable`-annotated function fails the build with a pointed error;
     * the same problem on a plain `@Preview` function just skips it.
     */
    override fun validate(elements: Set<ComposeHolder>) = elements.filter {
        it.skip.not() && verifyParameters(it) && verifyScope(it) && verifyProviderScope(it)
    }.toSet()
        .also {
            logger.info("Validated composable to only ${it.size}")
        }

    private fun verifyScope(holder: ComposeHolder): Boolean {
        if (holder.function.hasValidScope) {
            return true
        }
        report(
            holder = holder,
            problem = "'${holder.packageName}.${holder.functionName}' must not be private or protected — " +
                "Arkive generates code that calls it. Make it internal or public.",
        )
        return false
    }

    private fun verifyProviderScope(holder: ComposeHolder): Boolean {
        val provider = holder.parameters.firstNotNullOfOrNull { it.previewParameterProvider() } ?: return true
        if (provider.hasValidScope) {
            return true
        }
        report(
            holder = holder,
            problem = "PreviewParameterProvider '${provider.simpleName.asString()}' used by " +
                "'${holder.packageName}.${holder.functionName}' must not be private — " +
                "Arkive generates code that instantiates it. Make it internal or public.",
        )
        return false
    }

    private fun report(holder: ComposeHolder, problem: String) {
        if (holder.fromArkive) {
            logger.error("Arkive: $problem", holder.function)
        } else {
            logger.info("Arkive: skipping @Preview — $problem")
        }
    }

    /**
     * Check if there is no parameters or only one parameter with annotation `@PreviewParameter`
     */
    private fun verifyParameters(composeHolder: ComposeHolder): Boolean {
        val parameters = composeHolder.parameters
        return parameters.isEmpty() ||
            (parameters.size == 1 && parameters[0].previewParameterProvider() != null)
    }

    private fun KSValueParameter.previewParameterProvider(): KSClassDeclaration? {
        val annotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() in PREVIEW_PARAMETER_ANNOTATION_NAMES
        } ?: return null
        val provider = annotation.arguments.firstOrNull { it.name?.asString() == "provider" }?.value as? KSType
        return provider?.declaration as? KSClassDeclaration
    }
}
