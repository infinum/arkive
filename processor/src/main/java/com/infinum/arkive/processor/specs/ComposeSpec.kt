package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.infinum.arkive.processor.logger
import com.infinum.arkive.processor.models.ComposeHolder
import com.infinum.arkive.processor.shared.Constants
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

class ComposeSpec(
    private val codeGenerator: CodeGenerator,
    private val holders: Set<ComposeHolder>,
) : Spec {
    override fun write() {
        val fileSpec = getFileSpec()
        val testAnnotation = ClassName("org.junit", "Test")


        val arkiveClass = getArkiveClass()

        val runTestsFunction = FunSpec.builder("runTests")
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
                      """
                          runner("${getFunctionId(holder)}") {
                           ${getCodeBody(holder).toString().trimIndent()}
                          }
                      """.trimIndent()
                    }.joinToString(separator = "\n")
            )
            .build()

        arkiveClass.addFunction(runTestsFunction)

        logger.warn("Holders: ${holders}")
//        holders
//            .map { holder ->
//                FunSpec.builder("runTests")
//                    .addParameter()
//                    .addCode(getCodeBody(holder))
//                    .build()
//            }.forEach {
//                arkiveClass.addFunction(it)
//            }


        fileSpec
            .addType(arkiveClass.build())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )
    }

    fun getArkiveClass(): TypeSpec.Builder {
//        val ruleAnnotation = ClassName("org.junit", "Rule")
//        val paparazziClass = ClassName("app.cash.paparazzi", "Paparazzi")
//        val renderingModeClass =
//            ClassName("com.android.ide.common.rendering.api.SessionParams", "RenderingMode")
//
//        val paparazziProperty = PropertySpec.builder("paparazzi", paparazziClass)
//            .initializer("%T(\nrenderingMode = %T.SHRINK\n)", paparazziClass, renderingModeClass)
//            .addAnnotation(
//                AnnotationSpec.builder(ruleAnnotation)
//                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
//                    .build()
//            )
//            .build()

        return TypeSpec.classBuilder("ArkiveShoot")
//            .addProperty(paparazziProperty)

    }

    override fun getFileSpec(): FileSpec.Builder {
        return FileSpec.builder(
            Constants.PACKAGE_NAME,
            SIMPLE_NAME,
        )
    }

    // This id should be used in the generated json file to include more info about the component
    private fun getFunctionId(holder: ComposeHolder): String {
        val validPackageName = holder.packageName.replace(".", "")
        return "${validPackageName}_${holder.name}"
    }

    private fun getFunctionName(holder: ComposeHolder): String {
        return "Arkive_${getFunctionId(holder)}"

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