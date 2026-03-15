package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame

class PowerBlockRegistrationTest {

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
        TestHelper.initPowerFactory()
        val ids = PowerBlockFactory.getRegisteredBlockIds()

        // 1 solar + 6 drill + 4 battery + 6 cable + 2 lava generator + 8 auto smelter + 12 multi power cable + 2 cobblestone generator + 2 obsidian generator + 12 power merger = 55
        assertEquals(55, ids.size)
    }

    @Test
    fun `solar panel ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(PowerBlockFactory.isRegistered("small_solar_panel"))
    }

    @Test
    fun `all drill directional IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in SmallDrill.ALL_DIRECTIONAL_IDS) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing drill ID: $id")
        }
    }

    @Test
    fun `all battery variant IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in SmallBattery.ALL_VARIANT_IDS) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing battery ID: $id")
        }
    }

    @Test
    fun `all cable directional IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in PowerCable.DIRECTIONAL_IDS.values) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing cable ID: $id")
        }
    }

    @Test
    fun `solar panel ID creates SmallSolarPanel`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("small_solar_panel", TestHelper.createLocation())
        assertTrue(block is SmallSolarPanel)
    }

    @Test
    fun `drill ID creates SmallDrill`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("small_drill_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is SmallDrill)
    }

    @Test
    fun `battery ID creates SmallBattery`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("small_battery", TestHelper.createLocation(), BlockFace.DOWN)
        assertTrue(block is SmallBattery)
    }

    @Test
    fun `cable ID creates PowerCable`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("power_cable_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is PowerCable)
    }
}
