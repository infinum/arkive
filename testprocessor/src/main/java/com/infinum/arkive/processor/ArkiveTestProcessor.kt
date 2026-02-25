package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

private const val PACKAGE_NAME = "com.infinum.arkive"
private const val FILE_NAME = "ArkiveSnapshotTestGenerator"

class ArkiveTestProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var processed = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        FileSpec.builder(
            PACKAGE_NAME,
            FILE_NAME,
        ).addType(generateTestClass())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )

        return emptyList()
    }

    private fun generateTestClass(): TypeSpec {
        val ruleProperty = PropertySpec.builder(
            "paparazzi",
            ClassName("app.cash.paparazzi", "Paparazzi"),
        )
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit", "Rule"))
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    .build(),
            )
            .initializer(
                "Paparazzi(renderingMode = com.android.ide.common.rendering.api.SessionParams.RenderingMode.SHRINK)",
            )
            .build()

        val composableTestFunction = FunSpec.builder("testAllComposableFunctions")
            .addAnnotation(getTestAnnotation())
            .addCode(
                """
                val shooter = ArkiveComposeShoot()
                shooter.runComposableTests { name, function ->
                    paparazzi.snapshot(name = name) {
                        function()
                    }
                }
                """.trimIndent(),
            )
            .build()

        val frameLayoutClass = ClassName("android.widget", "FrameLayout")
        val layoutInflaterClass = ClassName("android.view", "LayoutInflater")

        val viewTestFunction = FunSpec.builder("testAllViewFunctions")
            .addAnnotation(getTestAnnotation())
            .addCode(
                """
                val shooter = ArkiveViewShoot()
                shooter.runViewTests { name, function ->
                    val viewId = function()
                    val view = %T.from(paparazzi.context).inflate(viewId, %T(paparazzi.context))
                    paparazzi.snapshot(view = view, name = name)
                }
                """.trimIndent(),
                layoutInflaterClass,
                frameLayoutClass,
            )
            .build()

        return TypeSpec.classBuilder(FILE_NAME)
            .addModifiers(KModifier.PUBLIC)
            .addProperty(ruleProperty)
            .addFunction(composableTestFunction)
            .addFunction(viewTestFunction)
            .build()
    }

    private fun getTestAnnotation() = ClassName("org.junit", "Test")
}

class ArkiveTestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor = ArkiveTestProcessor(environment.codeGenerator, environment.logger)
}
