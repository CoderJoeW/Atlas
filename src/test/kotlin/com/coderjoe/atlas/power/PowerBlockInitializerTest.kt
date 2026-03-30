package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.utility.block.SmallDrill
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

        // SmallSolarPanel: 2 (base + full)
        // SmallDrill: 1
        // SmallBattery: 5 (base + low + medium + high + full)
        // PowerCable: 1
        // LavaGenerator: 2 (base + active)
        // AutoSmelter: 1
        // PowerSplitter: 1
        // CobblestoneFactory: 1
        // ObsidianFactory: 1
        // Crusher: 1
        // PowerMerger: 1
        // SoftTouchDrill: 1
        // ExperienceExtractor: 2 (base + active)
        // Total: 19
        assertEquals(20, ids.size)
    }

    @Test
    fun `solar panel ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_solar_panel"),
        )
    }

    @Test
    fun `drill base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:small_drill"),
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
            PowerBlockFactory.createPowerBlock(
                "atlas:small_solar_panel",
                TestHelper.createLocation(),
            )
        assertTrue(block is SmallSolarPanel)
    }

    @Test
    fun `drill ID creates SmallDrill`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:small_drill",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is SmallDrill)
    }

    @Test
    fun `battery ID creates SmallBattery`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
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
            PowerBlockFactory.createPowerBlock(
                "atlas:power_cable",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is PowerCable)
    }
}
