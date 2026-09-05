package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockInitializerTest {
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

        // SmallSolarPanel: 2 (base + active)
        // SmallBattery: 5 (base + low + medium + high + full)
        // PowerCable: 1
        // LavaGenerator: 2 (base + active)
        // AutoSmelter: 1
        // CobblestoneFactory: 1
        // ObsidianFactory: 1
        // Crusher: 1
        // Mines: 7 (coal, iron, redstone, gold, emerald, diamond, netherite)
        // Total: 21
        assertEquals(21, ids.size)
    }

    @Test
    fun `solar panel ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_solar_panel"),
        )
    }

    @Test
    fun `battery base and variant IDs are registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_battery"),
        )
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_battery_low"),
        )
        assertTrue(
            PowerBlockFactory.isRegistered(
                "atlas:small_battery_medium",
            ),
        )
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_battery_full"),
        )
    }

    @Test
    fun `cable base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:power_cable"),
        )
    }

    @Test
    fun `solar panel ID creates SmallSolarPanel`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:small_solar_panel",
                TestHelper.createLocation(),
            )
        assertTrue(block is SmallSolarPanel)
    }

    @Test
    fun `battery ID creates SmallBattery`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:small_battery",
                TestHelper.createLocation(),
                BlockFace.DOWN,
            )
        assertTrue(block is SmallBattery)
    }

    @Test
    fun `cable ID creates PowerCable`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:power_cable",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is PowerCable)
    }
}
