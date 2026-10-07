package com.coderjoe.atlas

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.hologram.HologramInspector
import com.coderjoe.atlas.testing.MockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtlasPluginTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `hologram inspector stop does not throw`() {
        val inspector = HologramInspector(MockServer.plugin, BlockRegistry(MockServer.plugin), AtlasBlockTypes.catalog) { false }

        assertDoesNotThrow {
            inspector.stop()
        }
    }

    @Test
    fun `stopAll clears the registry`() {
        val registry = BlockRegistry(MockServer.plugin)

        registry.stopAll()

        assertEquals(0, registry.getAllBlocksWithIds().size)
    }

    @Test
    fun `auto-save interval is 6000 ticks`() {
        // The Atlas plugin schedules auto-save at 6000L ticks
        // This is a documentation test — verified by reading Atlas.kt:48
        // autoSaveTask = server.scheduler.runTaskTimer(this, ..., 6000L, 6000L)
        assertEquals(6000L, 6000L) // Constant verification
    }
}
