package com.infinum.arkive.processor.specs

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import java.lang.reflect.Proxy
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeRunnerSpecTest {

    // Whitespace-normalized: KotlinPoet wraps long lines at spaces.
    private val source = ComposeRunnerSpec(
        codeGenerator = unused(),
        holders = emptySet(),
        appPackages = setOf("com.app", "com.app.ui"),
        logger = unused(),
    ).fileSpec().toString().replace(Regex("\\s+"), " ")

    @Test
    fun everyShotGoesThroughUiTreeCapture() {
        assertTrue(source, source.contains("import com.infinum.arkive.composeutils.uitree.ArkiveUiTreeCapture"))
        assertTrue(source, source.contains("runner(name) { ArkiveUiTreeCapture(name, appPackages, content) }"))
    }

    @Test
    fun theModulePackagesAreBakedIn() {
        assertTrue(source, source.contains("private val appPackages: Set<String> = setOf(\"com.app\", \"com.app.ui\")"))
    }

    private inline fun <reified T : Any> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            error("unexpected call to ${method.name}")
        } as T
}
