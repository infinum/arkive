package com.infinum.arkive.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_PREVIEW_PARAMETERS
import com.infinum.arkive.plugin.extensions.ArkiveExtension.Companion.ENABLE_VARIANTS
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask
import com.infinum.arkive.plugin.tasks.GenerateShowcaseTask.Companion.RECORDING_TASK
import com.infinum.arkive.plugin.tasks.GenerateWebShowcaseTask
import com.infinum.arkive.plugin.utils.ArkiveVersion
import com.infinum.arkive.plugin.utils.capFirst
import org.gradle.api.Plugin
import org.gradle.api.Project

class ArkivePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project == project.rootProject) {
            // Root application hosts the aggregate task. It must be registered eagerly:
            // with org.gradle.configureondemand, task-name resolution happens right after
            // the root project configures — before any projectsEvaluated callback fires —
            // so callback-time registration is invisible to it. Children are forced from
            // afterEvaluate (not mid-script, which would reorder root-side configuration),
            // and the module task dependencies resolve lazily once they exist.
            project.afterEvaluate {
                project.evaluationDependsOnChildren()
            }
            project.tasks.register(
                GenerateWebShowcaseTask.NAME,
                GenerateWebShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateWebShowcaseTask.GROUP
                task.description = GenerateWebShowcaseTask.DESCRIPTION
                task.setSource(project.projectDir)
                task.dependsOn(project.provider { moduleShowcaseTaskPaths(project) })
            }
            return
        }

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

            project.afterEvaluate {
                val enablePreviewParameters = extension.enablePreviewParameters.get()
                val enableVariants = extension.enableVariants.get()

                project.extensions.findByName("ksp")?.let { kspExt ->
                    try {
                        val argMethod = kspExt.javaClass.getMethod("arg", String::class.java, String::class.java)
                        argMethod.invoke(kspExt, ENABLE_PREVIEW_PARAMETERS, enablePreviewParameters.toString())
                    } catch (e: Exception) {
                        project.logger.warn("Failed to pass $ENABLE_PREVIEW_PARAMETERS to KSP: ${e.message}")
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
        project.logger.info("Adding plugins")
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
        val arkiveVersion = ArkiveVersion.current
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
        project.logger.warn("Arkive: Adding tasks")

        project.plugins.withId("com.android.application") {
            project.logger.warn("Arkive: Android app")

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
                GenerateShowcaseTask::class.java,
            ) { task ->
                task.group = GenerateShowcaseTask.GROUP
                task.description = GenerateShowcaseTask.DESCRIPTION
                task.variant = variant
                val extension = project.extensions.findByType(ArkiveExtension::class.java)
                task.designFileKey = extension?.designFileKey?.get().orEmpty()
                task.keepSnapshots = extension?.keepSnapshots?.get() ?: false
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
            // Root and module applications may both schedule this; only register once.
            if (rootProject.tasks.findByName(GenerateWebShowcaseTask.NAME) == null) {
                rootProject.tasks.register(
                    GenerateWebShowcaseTask.NAME,
                    GenerateWebShowcaseTask::class.java,
                ) { task ->
                    task.group = GenerateWebShowcaseTask.GROUP
                    task.description = GenerateWebShowcaseTask.DESCRIPTION
                    task.dependsOn(moduleShowcaseTaskPaths(rootProject))
                    task.setSource(rootProject.projectDir)
                }
            }
        }
    }

    private fun moduleShowcaseTaskPaths(rootProject: Project): List<String> {
        return rootProject.subprojects
            .filter { it.pluginManager.hasPlugin(PLUGIN_ID) }
            .mapNotNull { module ->
                val variant = module.extensions.findByType(ArkiveExtension::class.java)
                    ?.multiModuleVariant?.get().orEmpty()
                module.tasks.findByName("${GenerateShowcaseTask.NAME}${variant.capFirst}")?.path
            }
    }

    companion object {
        private const val PLUGIN_ID = "com.infinum.arkive"
    }
}
