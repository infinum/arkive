package com.infinum.arkive.processor.models

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

data class ComposeHolder(
    override val name: String,
    override val functionName: String,
    override val packageName: String,
    override val skip: Boolean,
    override val group: String,
    override val tags: List<String>,
    override val extraMetadata: List<String>,
    override val figmaNodeId: String?,
    override val function: KSFunctionDeclaration,
    override val parameters: List<KSValueParameter>,
    override val fileName: String,
    // True when collected from @ArkiveComposable; false for plain @Preview functions.
    val fromArkive: Boolean = false,
) : Holder {
    // This id should be used in the generated json file to include more info about the component.
    // Dash-joined: '-' cannot appear in a package segment or function name, so two distinct
    // components can never produce the same id ('_' could — com.foo + Card_Header and
    // com.foo.card + Header both collapsed to com_foo_card_header), and snapshot-filename
    // parsing can rely on '_' marking only the id↔variant boundaries.
    override val functionId: String
        get() {
            val validPackageName = packageName.replace('.', '-')
            return "$validPackageName-$functionName".lowercase()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ComposeHolder) {
            return false
        }
        return functionId == other.functionId
    }

    override fun hashCode(): Int = functionId.hashCode()
}
