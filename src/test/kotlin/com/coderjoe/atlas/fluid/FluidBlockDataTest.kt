package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockDataTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `fromFluidBlock for FluidPump`() {
        val pump = FluidPump(TestHelper.createLocation(1.0, 2.0, 3.0))
        pump.storeFluid(FluidType.WATER)
        val data = FluidBlockData.fromFluidBlock(pump, "atlas:fluid_pump")

        assertEquals("atlas:fluid_pump", data.blockId)
        assertEquals("world", data.world)
        assertEquals(1, data.x)
        assertEquals(2, data.y)
        assertEquals(3, data.z)
        assertEquals("WATER", data.fluidType)
        assertNull(data.facing)
    }

    @Test
    fun `fromFluidBlock for FluidPipe captures facing`() {
        val pipe = FluidPipe(TestHelper.createLocation(), BlockFace.EAST)
        val data = FluidBlockData.fromFluidBlock(pipe, "atlas:fluid_pipe")
        assertEquals("EAST", data.facing)
    }

    @Test
    fun `toBlockFace with valid string`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "NONE", facing = "WEST")
        assertEquals(BlockFace.WEST, data.toBlockFace())
    }

    @Test
    fun `toBlockFace with null returns SELF`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "NONE", facing = null)
        assertEquals(BlockFace.SELF, data.toBlockFace())
    }

    @Test
    fun `toBlockFace with invalid returns SELF`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "NONE", facing = "INVALID")
        assertEquals(BlockFace.SELF, data.toBlockFace())
    }

    @Test
    fun `toFluidType WATER`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "WATER")
        assertEquals(FluidType.WATER, data.toFluidType())
    }

    @Test
    fun `toFluidType LAVA`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "LAVA")
        assertEquals(FluidType.LAVA, data.toFluidType())
    }

    @Test
    fun `toFluidType NONE`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "NONE")
        assertEquals(FluidType.NONE, data.toFluidType())
    }

    @Test
    fun `toFluidType invalid defaults to NONE`() {
        val data = FluidBlockData("id", "world", 0, 0, 0, "INVALID")
        assertEquals(FluidType.NONE, data.toFluidType())
    }

    @Test
    fun `toLocation with valid world`() {
        val data = FluidBlockData("id", "world", 5, 64, 10, "NONE")
        val loc = data.toLocation(TestHelper.mockPlugin)
        assertNotNull(loc)
        assertEquals(5.0, loc!!.x)
        assertEquals(64.0, loc.y)
        assertEquals(10.0, loc.z)
    }

    @Test
    fun `toLocation with invalid world returns null`() {
        val data = FluidBlockData("id", "nonexistent", 0, 0, 0, "NONE")
        assertNull(data.toLocation(TestHelper.mockPlugin))
    }

    @Test
    fun `round-trip FluidPipe preserves all fields`() {
        val pipe = FluidPipe(TestHelper.createLocation(1.0, 2.0, 3.0), BlockFace.NORTH)
        pipe.storeFluid(FluidType.LAVA)
        val data = FluidBlockData.fromFluidBlock(pipe, "atlas:fluid_pipe")

        assertEquals("atlas:fluid_pipe", data.blockId)
        assertEquals("NORTH", data.facing)
        assertEquals("LAVA", data.fluidType)
        assertEquals(BlockFace.NORTH, data.toBlockFace())
        assertEquals(FluidType.LAVA, data.toFluidType())
    }
}
