package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

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
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        val ids = FluidBlockFactory.getRegisteredBlockIds()

        // 3 pump + 6 directional pipe + 6 water-filled + 6 lava-filled = 21
        assertEquals(21, ids.size)
    }

    @Test
    fun `pump IDs are registered`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID))
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID_ACTIVE))
        assertTrue(FluidBlockFactory.isRegistered(FluidPump.BLOCK_ID_ACTIVE_LAVA))
    }

    @Test
    fun `all pipe directional IDs are registered`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        for (id in FluidPipe.DIRECTIONAL_IDS.values) {
            assertTrue(FluidBlockFactory.isRegistered(id), "Missing: $id")
        }
    }

    @Test
    fun `all pipe water-filled IDs are registered`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        for (id in FluidPipe.WATER_FILLED_IDS.values) {
            assertTrue(FluidBlockFactory.isRegistered(id), "Missing: $id")
        }
    }

    @Test
    fun `all pipe lava-filled IDs are registered`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        for (id in FluidPipe.LAVA_FILLED_IDS.values) {
            assertTrue(FluidBlockFactory.isRegistered(id), "Missing: $id")
        }
    }

    @Test
    fun `pump ID creates FluidPump`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        val block = FluidBlockFactory.createFluidBlock("fluid_pump", TestHelper.createLocation())
        assertTrue(block is FluidPump)
    }

    @Test
    fun `pipe ID creates FluidPipe`() {
        FluidBlockInitializer.initialize(TestHelper.mockPlugin)
        val block = FluidBlockFactory.createFluidBlock("fluid_pipe_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is FluidPipe)
    }
}
