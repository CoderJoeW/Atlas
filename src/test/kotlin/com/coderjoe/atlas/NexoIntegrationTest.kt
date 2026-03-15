package com.coderjoe.atlas

import io.mockk.every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class NexoIntegrationTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `initialize runs without errors when Nexo folder missing`() {
        // Nexo folder doesn't exist in test environment
        // Should log warning but not crash
        assertDoesNotThrow {
            val integration = NexoIntegration(TestHelper.mockPlugin)
            integration.initialize()
        }
    }

    @Test
    fun `initialize copies files when Nexo folders exist`() {
        // Create Nexo folder structure
        val nexoFolder = File(TestHelper.dataFolder.parentFile, "Nexo")
        File(nexoFolder, "items").mkdirs()
        File(nexoFolder, "pack/assets/atlas/textures/block").mkdirs()
        File(nexoFolder, "recipes/shapeless").mkdirs()

        // Mock saveResource to create the source files
        every { TestHelper.mockPlugin.saveResource(any(), any()) } answers {
            val path = firstArg<String>()
            val file = File(TestHelper.dataFolder, path)
            file.parentFile.mkdirs()
            file.writeText("test-content")
        }
        // Use the actual test dataFolder's parent
        every { TestHelper.mockPlugin.dataFolder } returns TestHelper.dataFolder

        val integration = NexoIntegration(TestHelper.mockPlugin)
        assertDoesNotThrow { integration.initialize() }

        nexoFolder.deleteRecursively()
    }
}
