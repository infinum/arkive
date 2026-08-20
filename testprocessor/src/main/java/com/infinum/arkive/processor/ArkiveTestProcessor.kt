package com.infinum.arkive.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.infinum.arkive.processor.generators.EngineTestGenerator
import com.infinum.arkive.processor.generators.PaparazziTestGenerator
import com.infinum.arkive.processor.generators.RoborazziTestGenerator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates the snapshot test class. Everything engine-specific lives in the selected
 * [EngineTestGenerator] — this processor only reads the `arkive.engine` KSP arg
 * (forwarded by the Arkive Gradle plugin) and writes whatever the generator builds.
 */
class ArkiveTestProcessor internal constructor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val generator: EngineTestGenerator,
) : SymbolProcessor {
    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) {
            return emptyList()
        }
        processed = true

        FileSpec.builder(
            EngineTestGenerator.PACKAGE_NAME,
            EngineTestGenerator.TEST_CLASS_NAME,
        ).addType(generator.generate())
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
            )

        return emptyList()
    }

    companion object {
        const val ENGINE_OPTION = "arkive.engine"
        const val ENGINE_PAPARAZZI = "paparazzi"
        const val DEVICE_OPTION = "arkive.device"
    }
}

class ArkiveTestProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor = ArkiveTestProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
        generator = when (environment.options[ArkiveTestProcessor.ENGINE_OPTION]) {
            ArkiveTestProcessor.ENGINE_PAPARAZZI -> PaparazziTestGenerator()
            else -> RoborazziTestGenerator(
                deviceQualifiers = environment.options[ArkiveTestProcessor.DEVICE_OPTION].orEmpty(),
            )
        },
    )
}
