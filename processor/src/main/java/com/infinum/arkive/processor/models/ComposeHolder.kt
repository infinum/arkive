package com.infinum.arkive.processor.models

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

data class ComposeHolder(
    val name: String,
    val functionName: String,
    val packageName: String,
    val skip: Boolean,
    val group: String,
    val tags: List<String>,
    val extraMetadata: List<String>,
    val function: KSFunctionDeclaration,
    val parameters: List<KSValueParameter>,
) {
    // This id should be used in the generated json file to include more info about the component
    val functionId: String
        get() {
            val validPackageName = packageName.replace(".", "_")
            return "${validPackageName}_$functionName".lowercase()
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
