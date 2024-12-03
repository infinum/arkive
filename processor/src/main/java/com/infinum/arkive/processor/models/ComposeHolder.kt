package com.infinum.arkive.processor.models

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

data class ComposeHolder(
    val name: String,
    val packageName: String,
    val function: KSFunctionDeclaration,
    val parameters: List<KSValueParameter>,
)
