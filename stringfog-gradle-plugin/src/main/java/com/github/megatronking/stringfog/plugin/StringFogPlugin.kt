package com.github.megatronking.stringfog.plugin

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import groovy.xml.XmlParser
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class StringFogPlugin : Plugin<Project> {

    companion object {
        private const val PLUGIN_NAME = "stringfog"
    }

    override fun apply(project: Project) {
        project.extensions.create(PLUGIN_NAME, StringFogExtension::class.java)

        // Try new DSL first (AGP 9+ with newDsl=true), fallback to old BaseExtension (AGP 8.x)
        val commonExtension = project.extensions.findByType(CommonExtension::class.java)
        val baseExtension = project.extensions.findByType(BaseExtension::class.java)
        if (commonExtension == null && baseExtension == null) {
            throw GradleException("StringFog plugin must be used with android plugin")
        }

        // Resolve namespace from whichever extension is available
        val namespace: String? = commonExtension?.namespace ?: baseExtension?.namespace

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            // Check stringfog extension
            val stringfog = project.extensions.getByType(StringFogExtension::class.java)
            if (stringfog.implementation.isNullOrEmpty()) {
                throw IllegalArgumentException("Missing stringfog implementation config")
            }
            if (!stringfog.enable) {
                return@onVariants
            }
            var applicationId: String? = null
            // We must get the package name to generate <package name>.StringFog.java
            // Priority: AndroidManifest -> namespace -> stringfog.packageName
            val manifestFile = project.file("src/main/AndroidManifest.xml")
            if (manifestFile.exists()) {
                val parsedManifest = XmlParser().parse(
                    InputStreamReader(FileInputStream(manifestFile), "utf-8")
                )
                if (!manifestFile.exists()) {
                    throw IllegalArgumentException("Failed to parse file $manifestFile")
                }
                applicationId = parsedManifest.attribute("package")?.toString()
            }
            if (applicationId.isNullOrEmpty()) {
                applicationId = namespace
            }
            if (applicationId.isNullOrEmpty()) {
                applicationId = stringfog.packageName
            }
            if (applicationId.isNullOrEmpty()) {
                throw IllegalArgumentException("Unable to resolve applicationId")
            }

            val logs = mutableListOf<String>()
            variant.instrumentation.transformClassesWith(
                StringFogTransform::class.java,
                InstrumentationScope.PROJECT
            ) { params ->
                params.setParameters(
                    applicationId,
                    stringfog,
                    logs,
                    "$applicationId.${SourceGeneratingTask.FOG_CLASS_NAME}"
                )
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )

            // Register source generation using Gradle 9+ compatible API
            val generateTaskName = "generateStringFog${variant.name.replaceFirstChar { it.uppercaseChar() }}"
            if (project.getTasksByName(generateTaskName, true).isEmpty()) {
                val stringfogDir = project.layout.buildDirectory.dir(
                    "generated/source/stringFog/${variant.name.lowercase()}"
                )
                val provider = project.tasks.register(generateTaskName, SourceGeneratingTask::class.java) { task ->
                    task.outputDir.set(stringfogDir)
                    task.applicationId.set(applicationId)
                    task.implementation.set(stringfog.implementation)
                    task.mode.set(stringfog.mode)
                }
                // Add generated source directory to variant's java sources
                variant.sources.java?.addGeneratedSourceDirectory(
                    provider,
                    SourceGeneratingTask::outputDir
                )
            }
            // TODO Need a final task to write logs to file
//            val printFile = File(project.buildDir, "outputs/mapping/${variant.name.lowercase()}/stringfog.txt")
//            printFile.writeText(logs.joinToString("\n"))
        }
    }

}