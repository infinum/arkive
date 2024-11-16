package com.infinum.arkive.plugin

import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

class ArkivePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            addPlugins(this)
            addDependencies(this)
            addTasks(this)
        }
    }

    private fun addPlugins(project: Project) {
        if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
            project.buildscript.dependencies.add(
                "classpath",
                "app.cash.paparazzi:paparazzi-gradle-plugin:1.3.4"
            )
            project.pluginManager.apply("app.cash.paparazzi")
        }

        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
            project.buildscript.dependencies.add(
                "classpath",
                "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.20-1.0.25"
            )
            project.pluginManager.apply("com.google.devtools.ksp")
        }
    }

    private fun addDependencies(project: Project) {
        // TODO: automate version
        val arkiveVersion = "0.0.1"
        with(project) {
            dependencies.add(
                "implementation",
                "com.infinum.arkive:annotations:$arkiveVersion"
            )
            dependencies.add(
                "kspDebug",
                "com.infinum.arkive:processor:$arkiveVersion"
            )
            dependencies.add(
                "kspTestDebug",
                "com.infinum.arkive:testprocessor:$arkiveVersion"
            )

            dependencies.add(
                "testImplementation",
                "junit:junit:4.13.2"
            )

            dependencies.add(
                "testRuntimeOnly",
                "org.junit.vintage:junit-vintage-engine:5.9.1"
            )
        }
    }


    fun Project.hasDependency(configurationName: String, group: String, name: String): Boolean {
        val configuration = configurations.findByName(configurationName) ?: return false
        return configuration.dependencies.any { dependency ->
            dependency.group == group && dependency.name == name
        }
    }

    private fun addTasks(project: Project) {
        with(project) {
            tasks.register(GenerateShowcaseTask.NAME, GenerateShowcaseTask::class.java) { task ->
                task.group = GenerateShowcaseTask.GROUP
                task.description = GenerateShowcaseTask.DESCRIPTION
                task.setSource(projectDir)
            }

            tasks.register(
                GenerateWebShowcaseTask.NAME,
                GenerateWebShowcaseTask::class.java
            ) { task ->
                task.group = GenerateWebShowcaseTask.GROUP
                task.description = GenerateWebShowcaseTask.DESCRIPTION
                task.setSource(projectDir)
            }
        }
    }
}
