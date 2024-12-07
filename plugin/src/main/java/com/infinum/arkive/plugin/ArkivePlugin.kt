package com.infinum.arkive.plugin

import app.cash.paparazzi.gradle.PaparazziPlugin
import com.infinum.arkive.plugin.extensions.ArkiveExtension
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger

class ArkivePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            addExtensions(this)
            addPlugins(this)
            addDependencies(this)
            addTestDependencies(this)
            addTasks(this)
        }
    }

    private fun addExtensions(project: Project) {
        project.plugins.withId("com.android.base") {
            project.extensions.create(
                "arkive",
                ArkiveExtension::class.java,
            )
        }
    }

    private fun addPlugins(project: Project) {
        logger.info("Adding plugins")
        if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
            project.pluginManager.apply(PaparazziPlugin::class.java)
        }

        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
            project.pluginManager.apply("com.google.devtools.ksp")
        }
    }

    private fun addDependencies(project: Project) {
        // TODO: automate version
        val arkiveVersion = "0.0.1"
        with(project) {
            dependencies.add(
                "implementation",
                "com.infinum.arkive:annotations:$arkiveVersion",
            )
            dependencies.add(
                "kspDebug",
                "com.infinum.arkive:processor:$arkiveVersion",
            )
            dependencies.add(
                "kspTestDebug",
                "com.infinum.arkive:testprocessor:$arkiveVersion",
            )
        }
    }

    private fun addTestDependencies(project: Project) {
        val testDependencies = listOf(
            "junit:junit:4.13.2" to "testImplementation",
            "org.junit.vintage:junit-vintage-engine:5.9.1" to "testRuntimeOnly",
        )

        testDependencies.forEach { (dependencyNotation, configurationName) ->
            val configuration = project.configurations.getByName(configurationName)

            val dependencyExists = configuration.dependencies.any { dependency ->
                dependency.group == dependencyNotation.substringBefore(":") &&
                        dependency.name == dependencyNotation.substringAfter(":")
                    .substringBefore(":")
            }

            if (!dependencyExists) {
                project.dependencies.add(configurationName, dependencyNotation)
            }
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
                GenerateWebShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateWebShowcaseTask.GROUP
                task.description = GenerateWebShowcaseTask.DESCRIPTION
                task.setSource(projectDir)
            }
        }
    }
}
