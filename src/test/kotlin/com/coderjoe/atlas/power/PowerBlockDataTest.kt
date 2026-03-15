package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockDataTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `fromPowerBlock for SmallSolarPanel`() {
        val loc = TestHelper.createLocation(10.0, 64.0, 20.0)
        val panel = SmallSolarPanel(loc)
        panel.currentPower = 1
        val data = PowerBlockData.fromPowerBlock(panel, "small_solar_panel")

        assertEquals("small_solar_panel", data.blockId)
        assertEquals("world", data.world)
        assertEquals(10, data.x)
        assertEquals(64, data.y)
        assertEquals(20, data.z)
        assertEquals(1, data.currentPower)
        assertNull(data.facing)
        assertNull(data.enabled)
    }

    @Test
    fun `fromPowerBlock for PowerCable captures facing`() {
        val cable = PowerCable(TestHelper.createLocation(), BlockFace.EAST)
        val data = PowerBlockData.fromPowerBlock(cable, "power_cable_east")
        assertEquals("EAST", data.facing)
    }

    @Test
    fun `fromPowerBlock for SmallDrill captures facing and enabled`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.NORTH)
        drill.enabled = false
        val data = PowerBlockData.fromPowerBlock(drill, "small_drill_north")
        assertEquals("NORTH", data.facing)
        assertEquals(false, data.enabled)
    }

    @Test
    fun `fromPowerBlock for SmallBattery captures facing`() {
        val battery = SmallBattery(TestHelper.createLocation(), BlockFace.UP)
        val data = PowerBlockData.fromPowerBlock(battery, "small_battery")
        assertEquals("UP", data.facing)
    }

    @Test
    fun `toBlockFace with valid facing string`() {
        val data = PowerBlockData("id", "world", 0, 0, 0, 0, facing = "NORTH")
        assertEquals(BlockFace.NORTH, data.toBlockFace())
    }

    @Test
    fun `toBlockFace with null facing returns SELF`() {
        val data = PowerBlockData("id", "world", 0, 0, 0, 0, facing = null)
        assertEquals(BlockFace.SELF, data.toBlockFace())
    }

    @Test
    fun `toBlockFace with invalid string returns SELF`() {
        val data = PowerBlockData("id", "world", 0, 0, 0, 0, facing = "INVALID")
        assertEquals(BlockFace.SELF, data.toBlockFace())
    }

    @Test
    fun `toLocation with valid world`() {
        val data = PowerBlockData("id", "world", 5, 64, 10, 0)
        val loc = data.toLocation(TestHelper.mockPlugin)
        assertNotNull(loc)
        assertEquals(5.0, loc!!.x)
        assertEquals(64.0, loc.y)
        assertEquals(10.0, loc.z)
    }

    @Test
    fun `toLocation with invalid world returns null`() {
        val data = PowerBlockData("id", "nonexistent_world", 0, 0, 0, 0)
        val loc = data.toLocation(TestHelper.mockPlugin)
        assertNull(loc)
    }

    @Test
    fun `round-trip SmallSolarPanel preserves all fields`() {
        val loc = TestHelper.createLocation(1.0, 2.0, 3.0)
        val panel = SmallSolarPanel(loc)
        panel.currentPower = 1
        val data = PowerBlockData.fromPowerBlock(panel, "small_solar_panel")

        assertEquals("small_solar_panel", data.blockId)
        assertEquals(1, data.x)
        assertEquals(2, data.y)
        assertEquals(3, data.z)
        assertEquals(1, data.currentPower)
    }

    @Test
    fun `round-trip SmallDrill preserves facing and enabled`() {
        val drill = SmallDrill(TestHelper.createLocation(), BlockFace.WEST)
        drill.enabled = true
        drill.currentPower = 5
        val data = PowerBlockData.fromPowerBlock(drill, "small_drill_west")

        assertEquals("WEST", data.facing)
        assertEquals(true, data.enabled)
        assertEquals(5, data.currentPower)
        assertEquals(BlockFace.WEST, data.toBlockFace())
    }
}
