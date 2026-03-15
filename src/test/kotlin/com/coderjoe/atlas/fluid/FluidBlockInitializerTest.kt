package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockInitializerTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `initialize registers all expected IDs`() {
        TestHelper.initFluidFactory()
        val ids = FluidBlockFactory.getRegisteredBlockIds()

        // FluidPump: 3 (base + active + active_lava)
        // FluidPipe: 1 (base only)
        // FluidContainer: 1 (base only)
        // FluidMerger: 1 (base only)
        // Total: 6
        assertEquals(6, ids.size)
    }

    @Test
    fun `pump IDs are registered`() {
        TestHelper.initFluidFactory()
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID))
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID_ACTIVE))
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID_ACTIVE_LAVA))
    }

    @Test
    fun `pipe base ID is registered`() {
        TestHelper.initFluidFactory()
        assertTrue(FluidBlockFactory.isRegistered(FluidPipe.BLOCK_ID))
    }

    @Test
    fun `pump ID creates FluidPump`() {
        TestHelper.initFluidFactory()
        val block =
            FluidBlockFactory.createFluidBlock(
                "atlas:fluid_pump",
                TestHelper.createLocation(),
            )
        assertTrue(block is FluidPump)
    }

    @Test
    fun `pipe ID creates FluidPipe`() {
        TestHelper.initFluidFactory()
        val block =
            FluidBlockFactory.createFluidBlock(
                "atlas:fluid_pipe",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is FluidPipe)
    }
}
