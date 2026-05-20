package com.github.megatronking.stringfog.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Functional tests for StringFogPlugin using Gradle TestKit.
 * Verifies plugin behavior under AGP 8.x and AGP 9.x using the Android Components APIs.
 */
class StringFogPluginFuncTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var pluginClasspath: List<File>

    @Before
    fun setUp() {
        // Load the plugin classpath from the testKit classpath resource
        val classpathResource = javaClass.getResourceAsStream("/plugin-classpath.txt")
        assertNotNull("plugin-classpath.txt resource not found. Ensure gradleTestKit() is in test dependencies.", classpathResource)
        pluginClasspath = classpathResource!!.bufferedReader().use { reader ->
            reader.readLines().map { File(it) }
        }
    }

    /**
     * Test 1: AGP 8.x regression test.
     * Verifies that the plugin applies successfully on AGP 8.0.0 through CommonExtension
     * and the generateStringFog task is registered.
     */
    @Test
    fun testPluginAppliesOnAgp8() {
        val result = setupAndRun("projects/agp8", "tasks", "--all")

        // Verify build configuration succeeds
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)

        // Verify the generateStringFog task is registered
        assertTrue(
            "generateStringFogDebug task should be registered",
            result.output.contains("generateStringFogDebug")
        )

        // Verify no ClassCastException from legacy Android extension APIs
        assertFalse(
            "Should not contain ClassCastException",
            result.output.contains("ClassCastException")
        )
    }

    /**
     * Test 2: AGP 9.x new DSL test.
     * Verifies that the plugin applies successfully on AGP 9.1.1 with android.newDsl=true
     * and does not throw legacy Android extension cast exceptions.
     */
    @Test
    fun testPluginAppliesOnAgp9NewDsl() {
        val result = setupAndRun("projects/agp9", "tasks", "--all")

        // Verify build configuration succeeds
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)

        // Verify the generateStringFog task is registered
        assertTrue(
            "generateStringFogDebug task should be registered",
            result.output.contains("generateStringFogDebug")
        )

        // Verify no ClassCastException from legacy Android extension APIs (critical for AGP 9.x new DSL)
        assertFalse(
            "Should not contain ClassCastException (AGP 9.x new DSL compatibility)",
            result.output.contains("ClassCastException")
        )
    }

    /**
     * Sets up a test project from fixture resources and runs Gradle with the given arguments.
     */
    private fun setupAndRun(fixturePath: String, vararg arguments: String): org.gradle.testkit.runner.BuildResult {
        // Copy fixture files to the temporary project directory
        val fixtureDir = File(javaClass.getResource("/$fixturePath")!!.toURI())
        copyDirectory(fixtureDir, testProjectDir.root)

        // Build the GradleRunner
        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath(pluginClasspath)
            .withArguments(*arguments)

        return runner.build()
    }

    /**
     * Recursively copies a directory's contents to a target directory.
     */
    private fun copyDirectory(source: File, target: File) {
        if (!target.exists()) {
            target.mkdirs()
        }
        source.listFiles()?.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                copyDirectory(file, targetFile)
            } else {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
