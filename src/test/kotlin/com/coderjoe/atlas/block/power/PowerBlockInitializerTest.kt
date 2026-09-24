package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.testing.Blocks
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockInitializerTest {
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
        Blocks.initPowerFactory()
        val ids = PowerBlockFactory.getRegisteredBlockIds()

        // SmallSolarPanel: 2 (base + active)
        // SmallBattery: 5 (base + low + medium + high + full)
        // PowerCable: 1
        // LavaGenerator: 2 (base + active)
        // CobblestoneFactory: 1
        // ObsidianFactory: 1
        // Mines: 7 (coal, iron, redstone, gold, emerald, diamond, netherite)
        // Total: 19
        assertEquals(19, ids.size)
    }

    @Test
    fun `solar panel ID is registered`() {
        Blocks.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_solar_panel"),
        )
    }

    @Test
    fun `battery base and variant IDs are registered`() {
        Blocks.initPowerFactory()
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
        Blocks.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:power_cable"),
        )
    }

    @Test
    fun `solar panel ID creates SmallSolarPanel`() {
        Blocks.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:small_solar_panel",
                MockServer.createLocation(),
            )
        assertTrue(block is SmallSolarPanel)
    }

    @Test
    fun `battery ID creates SmallBattery`() {
        Blocks.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:small_battery",
                MockServer.createLocation(),
                BlockFace.DOWN,
            )
        assertTrue(block is SmallBattery)
    }

    @Test
    fun `cable ID creates PowerCable`() {
        Blocks.initPowerFactory()
        val block =
            PowerBlockFactory.create(
                "atlas:power_cable",
                MockServer.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is PowerCable)
    }
}
