package com.infinum.arkive.processor.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

/**
 * One implementation per snapshot engine; each owns the complete shape of the generated
 * test class — annotations, properties, test methods, failure semantics. The processor
 * only selects an implementation; nothing about an engine leaks outside its generator.
 */
internal interface EngineTestGenerator {

    /** Builds the complete `ArkiveSnapshotTestGenerator` class for this engine. */
    fun generate(): TypeSpec

    companion object {
        const val PACKAGE_NAME = "com.infinum.arkive"
        const val TEST_CLASS_NAME = "ArkiveSnapshotTestGenerator"

        // Everything downstream (SnapshotsGrabber's boundary filter, the showcase
        // generator's filename parsing) keys on this prefix — both engines must use it.
        const val SNAPSHOT_FILE_PREFIX = "${PACKAGE_NAME}_${TEST_CLASS_NAME}_"

        const val JUNIT_PACKAGE = "org.junit"
        const val RETENTION_SYSTEM_PROPERTY = "arkive.snapshot.retention"

        fun testAnnotation() = ClassName(JUNIT_PACKAGE, "Test")
    }
}
