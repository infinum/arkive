package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.shared.Constants
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

class ComposeSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
) : Spec {
    override fun write() {
        val fileSpec = getFileSpec()
        val arkiveClass = getArkiveClass()
        arkiveClass.addFunction(getRunTestsFunction())

        fileSpec
            .addType(arkiveClass.build())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )
    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    private fun getRunnerFunction(holder: ComposeHolder): String {
        return """
            runner("${getFunctionId(holder)}") {
             ${getCodeBody(holder).toString().trimIndent()}
            } 
        """.trimIndent()

    }

    private fun getRunTestsFunction(): FunSpec {
        return FunSpec.builder("runTests")
            .addParameter(
                "runner",
                LambdaTypeName.get(
                    parameters = arrayOf(
                        ClassName("kotlin", "String"),
                        LambdaTypeName.get(returnType = UNIT)
                            .copy(annotations = listOf(getComposableAnnotation())),

                        ),
                    returnType = UNIT
                )
            )
            .addCode(
                holders
                    .map { holder ->
                        getRunnerFunction(holder)
                    }.joinToString(separator = "\n")
            )
            .build()
    }

    private fun getArkiveClass(): TypeSpec.Builder {
        return TypeSpec.classBuilder("ArkiveShoot")
    }

    // This id should be used in the generated json file to include more info about the component
    private fun getFunctionId(holder: ComposeHolder): String {
        val validPackageName = holder.packageName.replace(".", "_")
        return "${validPackageName}_${holder.name}"
    }

    private fun getCodeBody(holder: ComposeHolder): CodeBlock {
        return CodeBlock.builder().apply {
            val functionMember = MemberName(
                packageName = holder.packageName,
                simpleName = holder.name,
            )
            addStatement(
                "%M()", functionMember
            )
        }.build()
    }

    private fun getComposableAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(ClassName.bestGuess(ANNOTATION_COMPOSABLE))
            .build()
    }

    companion object {
        const val SIMPLE_NAME = "ArkiveShoot"
        const val ANNOTATION_COMPOSABLE = "androidx.compose.runtime.Composable"
    }
}