package com.infinum.arkive.plugin.utils

import com.infinum.arkive.plugin.uitree.UiTreeWiring
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

/**
 * Points composeUtils' UI-tree capture at the variant's tree directory — or, with
 * `uiTree` off, passes nothing, which makes the capture a pass-through. Like
 * RetentionArgumentProvider: a named class over a Provider keeps the Test task
 * configuration-cache safe, and `enabled` is a tracked input. The absolute directory is
 * kept out of the cache key; it is declared as a task output instead.
 */
internal class UiTreeArgumentProvider(
    @get:Input
    val enabled: Provider<Boolean>,
    @get:Internal
    val directory: String,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> =
        if (enabled.get()) listOf("-D${UiTreeWiring.DIRECTORY_PROPERTY}=$directory") else emptyList()
}
