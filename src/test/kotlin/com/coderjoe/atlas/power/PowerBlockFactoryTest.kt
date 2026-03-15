package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.SmallBattery
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
import org.junit.jupiter.api.Assertions.assertNotNull

class PowerBlockFactoryTest {

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
        PowerBlockFactory.register("test_block") { loc, _ -> SmallSolarPanel(loc) }
        assertTrue(PowerBlockFactory.isRegistered("test_block"))
    }

    @Test
    fun `isRegistered returns false for unknown ID`() {
        assertFalse(PowerBlockFactory.isRegistered("unknown_block"))
    }

    @Test
    fun `createPowerBlock returns correct instance`() {
        PowerBlockFactory.register("small_solar_panel") { loc, _ -> SmallSolarPanel(loc) }
        val block = PowerBlockFactory.createPowerBlock("small_solar_panel", TestHelper.createLocation())
        assertNotNull(block)
        assertTrue(block is SmallSolarPanel)
    }

    @Test
    fun `createPowerBlock returns null for unregistered ID`() {
        val block = PowerBlockFactory.createPowerBlock("unknown", TestHelper.createLocation())
        assertNull(block)
    }

    @Test
    fun `getRegisteredBlockIds returns all registered IDs`() {
        PowerBlockFactory.register("block_a") { loc, _ -> SmallSolarPanel(loc) }
        PowerBlockFactory.register("block_b") { loc, facing -> SmallBattery(loc, facing) }
        val ids = PowerBlockFactory.getRegisteredBlockIds()
        assertEquals(setOf("block_a", "block_b"), ids)
    }

    @Test
    fun `later registration overwrites earlier one`() {
        PowerBlockFactory.register("test_block") { loc, _ -> SmallSolarPanel(loc) }
        PowerBlockFactory.register("test_block") { loc, facing -> SmallBattery(loc, facing) }
        val block = PowerBlockFactory.createPowerBlock("test_block", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is SmallBattery)
    }
}
