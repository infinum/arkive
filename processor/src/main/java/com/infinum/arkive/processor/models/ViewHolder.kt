package com.infinum.arkive.processor.models

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

data class ViewHolder(
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
    override val fileName: String
) : Holder {
    // This id should be used in the generated json file to include more info about the component
    override val functionId: String
        get() {
            val validPackageName = packageName.replace(".", "_")
            return "${validPackageName}_$functionName".lowercase()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ViewHolder) {
            return false
        }
        return functionId == other.functionId
    }

    override fun hashCode(): Int = functionId.hashCode()
}
