package com.infinum.arkive.plugin

import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
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
            tasks.register(GenerateShowcaseTask.NAME, GenerateShowcaseTask::class.java) { task ->
                task.group = GenerateShowcaseTask.GROUP
                task.description = GenerateShowcaseTask.DESCRIPTION
                task.setSource(projectDir)
            }
        }
    }
}