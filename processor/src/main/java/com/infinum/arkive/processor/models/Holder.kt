package com.infinum.arkive.processor.models

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

interface Holder {
    val name: String
    val functionName: String
    val packageName: String
    val skip: Boolean
    val group: String
    val tags: List<String>
    val extraMetadata: List<String>
    val function: KSFunctionDeclaration
    val parameters: List<KSValueParameter>
    val functionId: String
}
