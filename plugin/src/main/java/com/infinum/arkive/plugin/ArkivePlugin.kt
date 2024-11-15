package com.infinum.arkive.plugin

import com.infinum.arkive.plugin.tasks.GenerateReportTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ArkivePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            addTasks(project)
        }
    }

    private fun addTasks(project: Project) {
        with(project) {
            tasks.register(GenerateReportTask.NAME, GenerateReportTask::class.java) { task ->
                task.group = GenerateReportTask.GROUP
                task.description = GenerateReportTask.DESCRIPTION
                task.setSource(projectDir)
            }
        }
    }
}