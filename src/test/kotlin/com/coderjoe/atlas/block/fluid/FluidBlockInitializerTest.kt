package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.testing.Blocks
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockInitializerTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `initialize registers all expected IDs`() {
        Blocks.initFluidFactory()
        val ids = FluidBlockFactory.getRegisteredBlockIds()

        // FluidPump: 1 (base only - connections and status are block state properties)
        // FluidPipe: 1 (base only)
        // FluidContainer: 1 (base only)
        // Total: 3
        assertEquals(3, ids.size)
    }

    @Test
    fun `pump ID is registered`() {
        Blocks.initFluidFactory()
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID))
    }

    @Test
    fun `pipe base ID is registered`() {
        Blocks.initFluidFactory()
        assertTrue(FluidBlockFactory.isRegistered(FluidPipe.BLOCK_ID))
    }

    @Test
    fun `pump ID creates FluidPump`() {
        Blocks.initFluidFactory()
        val block =
            FluidBlockFactory.create(
                "atlas:fluid_pump",
                MockServer.createLocation(),
            )
        assertTrue(block is FluidPump)
    }

    @Test
    fun `pipe ID creates FluidPipe`() {
        Blocks.initFluidFactory()
        val block =
            FluidBlockFactory.create(
                "atlas:fluid_pipe",
                MockServer.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is FluidPipe)
    }
}
