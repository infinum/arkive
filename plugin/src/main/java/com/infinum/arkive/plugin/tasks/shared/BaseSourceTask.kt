package com.infinum.arkive.plugin.tasks.shared

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskExecutionException

internal abstract class BaseSourceTask : SourceTask() {

    internal fun showError(message: String): Nothing = throw TaskExecutionException(this, Exception(message))
}
