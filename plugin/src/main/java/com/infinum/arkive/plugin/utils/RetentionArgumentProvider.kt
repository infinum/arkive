package com.infinum.arkive.plugin.utils

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

/**
 * Forwards the retention policy to the test JVM, where the generated snapshot test reads
 * it to decide which verify guards apply (`ArkiveTestProcessor` reads the same property
 * name — the two must stay in sync).
 *
 * A named class holding a [Provider] rather than a lambda over `Project`: nothing here
 * captures the project, so `Test` tasks stay serializable under the configuration cache,
 * and `@get:Input` makes the forwarded value a tracked task input — flipping
 * `snapshotRetention` re-runs the tests instead of leaving them UP-TO-DATE on the old
 * policy.
 */
internal class RetentionArgumentProvider(
    @get:Input
    val retention: Provider<String>,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> =
        listOf("-D$RETENTION_SYSTEM_PROPERTY=${retention.get()}")

    companion object {
        // Read by the generated test class; see ArkiveTestProcessor.retentionProperty().
        const val RETENTION_SYSTEM_PROPERTY = "arkive.snapshot.retention"
    }
}
