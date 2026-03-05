package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidBlockFactoryTest {

    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `register and isRegistered returns true`() {
        FluidBlockFactory.register("fluid_pump") { loc, _ -> FluidPump(loc) }
        assertTrue(FluidBlockFactory.isRegistered("fluid_pump"))
    }

    @Test
    fun `isRegistered returns false for unknown`() {
        assertFalse(FluidBlockFactory.isRegistered("unknown"))
    }

    @Test
    fun `createFluidBlock returns correct instance`() {
        FluidBlockFactory.register("fluid_pump") { loc, _ -> FluidPump(loc) }
        val block = FluidBlockFactory.createFluidBlock("fluid_pump", TestHelper.createLocation())
        assertNotNull(block)
        assertTrue(block is FluidPump)
    }

    @Test
    fun `createFluidBlock returns null for unknown`() {
        assertNull(FluidBlockFactory.createFluidBlock("unknown", TestHelper.createLocation()))
    }

    @Test
    fun `getRegisteredBlockIds returns all`() {
        FluidBlockFactory.register("a") { loc, _ -> FluidPump(loc) }
        FluidBlockFactory.register("b") { loc, facing -> FluidPipe(loc, facing) }
        assertEquals(setOf("a", "b"), FluidBlockFactory.getRegisteredBlockIds())
    }
}
