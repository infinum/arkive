package com.infinum.arkive.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.DISABLE_PREVIEW_PARAMETERS
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_VARIANTS
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask.Companion.RECORDING_TASK
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import com.infinum.arkive.plugin.utils.capFirst
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

            addRootTasks(rootProject)
        }
    }

    private fun addExtensions(project: Project) {
        project.plugins.withId("com.android.base") {
            val extension = project.extensions.create(
                "arkive",
                ArkiveExtension::class.java,
            )

            extension.disablePreviewParameters.convention(false)

            project.afterEvaluate {
                val disablePreviewParameters = extension.disablePreviewParameters.get()
                val enableVariants = extension.enableVariants.get()

                project.extensions.findByName("ksp")?.let { kspExt ->
                    try {
                        val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                        argMethod.invoke(kspExt, DISABLE_PREVIEW_PARAMETERS, disablePreviewParameters.toString())
                    } catch (e: Exception) {
                        project.logger.warn("Failed to pass $DISABLE_PREVIEW_PARAMETERS to KSP: ${e.message}")
                    }

                    try {
                        val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                        argMethod.invoke(kspExt, ENABLE_VARIANTS, enableVariants.toString())
                    } catch (e: Exception) {
                        project.logger.warn("Failed to pass enableVariants to KSP: ${e.message}")
                    }
                }
            }
        }
    }

    private fun addPlugins(project: Project) {
        logger.info("Adding plugins")
        if (!project.pluginManager.hasPlugin("app.cash.paparazzi")) {
            // Apply by id (not class reference) so Paparazzi need not be on the compile
            // classpath — it can stay an `implementation` dep, hidden from consumers.
            project.pluginManager.apply("app.cash.paparazzi")
        }

//        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
//            project.pluginManager.apply("com.google.devtools.ksp")
//        }
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
                "implementation",
                "com.infinum.arkive:composeUtils:$arkiveVersion",
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

        logger.warn("Arkive: Adding tasks")

        project.plugins.withId("com.android.application") {
            logger.warn("Arkive: Android app")

            val androidComponents =
                project.extensions.getByType(AndroidComponentsExtension::class.java)

            androidComponents.onVariants {
                addTaskWithVariant(project, it.name)
            }
        }

        project.plugins.withId("com.android.library") {
            val libraryComponents =
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)

            libraryComponents.onVariants {
                addTaskWithVariant(project, it.name)
            }
        }
    }

    private fun addTaskWithVariant(project: Project, variant: String) {
        with(project) {
            tasks.register(
                "${GenerateShowcaseTask.NAME}${variant.capFirst}",
                GenerateShowcaseTask::class.java
            ) { task ->
                task.group = GenerateShowcaseTask.GROUP
                task.description = GenerateShowcaseTask.DESCRIPTION
                task.variant = variant
                task.designFileKey = project.extensions.findByType(ArkiveExtension::class.java)
                    ?.designFileKey?.get().orEmpty()
                if (variant.isEmpty()) {
                    task.dependsOn(RECORDING_TASK)
                } else {
                    task.dependsOn("$RECORDING_TASK${variant.capFirst}")
                }
                task.setSource(projectDir)
            }
        }
    }

    private fun addRootTasks(rootProject: Project) {
        if (rootProject.tasks.findByName(GenerateWebShowcaseTask.NAME) != null) {
            return
        }
        rootProject.gradle.projectsEvaluated {
            val subTasks = rootProject.subprojects
                .filter {
                    it.pluginManager.hasPlugin("com.infinum.arkive")
                }.mapNotNull { module ->
                    val variant = module.extensions.findByType(ArkiveExtension::class.java)
                        ?.multiModuleVariant?.get().orEmpty()

                    module.tasks.findByName("${GenerateShowcaseTask.NAME}${variant.capFirst}")?.path
                }

            rootProject.tasks.register(
                GenerateWebShowcaseTask.NAME,
                GenerateWebShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateWebShowcaseTask.GROUP
                task.description = GenerateWebShowcaseTask.DESCRIPTION
                task.dependsOn(
                    *subTasks.toTypedArray()
                )
                task.setSource(rootProject.projectDir)
            }
        }
    }
}
