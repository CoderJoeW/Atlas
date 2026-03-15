package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.MultiPowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MultiPowerCableTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `multi power cable has correct facing`() {
        val cable =
            MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, cable.facing)
    }

    @Test
    fun `visual state always returns BLOCK_ID`() {
        val cable =
            MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        cable.currentPower = 0
        assertEquals(
            "atlas:multi_power_cable",
            cable.getVisualStateBlockId(),
        )
        cable.currentPower = 5
        assertEquals(
            "atlas:multi_power_cable",
            cable.getVisualStateBlockId(),
        )
    }

    @Test
    fun `base block ID is atlas multi_power_cable`() {
        val cable =
            MultiPowerCable(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("atlas:multi_power_cable", cable.baseBlockId)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = MultiPowerCable.descriptor
        assertEquals("atlas:multi_power_cable", desc.baseBlockId)
        assertEquals("Multi Power Cable", desc.displayName)
    }

    @Test
    fun `descriptor has directional placement`() {
        val desc = MultiPowerCable.descriptor
        assertEquals(
            com.coderjoe.atlas.core.PlacementType.DIRECTIONAL,
            desc.placementType,
        )
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:multi_power_cable"),
        )
    }

    @Test
    fun `factory creates MultiPowerCable from base ID`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:multi_power_cable",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is MultiPowerCable)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `max storage is 10`() {
        val cable =
            MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(10, cable.maxStorage)
    }

    @Test
    fun `pulls power from behind`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            cable,
            "atlas:multi_power_cable",
        )

        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        cable.callPowerUpdate()

        assertTrue(cable.currentPower > 0)
        assertTrue(battery.currentPower < 5)
    }

    @Test
    fun `distributes power to multiple outputs`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        cable.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            cable,
            "atlas:multi_power_cable",
        )

        val eastBatteryLoc =
            TestHelper.createLocation(1.0, 64.0, 0.0)
        val eastBattery = SmallBattery(eastBatteryLoc, BlockFace.WEST)
        TestHelper.addToRegistry(
            registry,
            eastBattery,
            "atlas:small_battery",
        )

        val westBatteryLoc =
            TestHelper.createLocation(-1.0, 64.0, 0.0)
        val westBattery = SmallBattery(westBatteryLoc, BlockFace.EAST)
        TestHelper.addToRegistry(
            registry,
            westBattery,
            "atlas:small_battery",
        )

        val northBatteryLoc =
            TestHelper.createLocation(0.0, 64.0, -1.0)
        val northBattery =
            SmallBattery(northBatteryLoc, BlockFace.SOUTH)
        TestHelper.addToRegistry(
            registry,
            northBattery,
            "atlas:small_battery",
        )

        cable.callPowerUpdate()

        assertTrue(eastBattery.currentPower > 0)
        assertTrue(westBattery.currentPower > 0)
        assertTrue(northBattery.currentPower > 0)
        assertEquals(2, cable.currentPower)
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        cable.currentPower = 10
        TestHelper.addToRegistry(
            registry,
            cable,
            "atlas:multi_power_cable",
        )

        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        cable.callPowerUpdate()

        assertEquals(10, cable.maxStorage)
        assertEquals(5, battery.currentPower)
    }

    @Test
    fun `does not crash with no adjacent blocks`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val cable =
            MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)

        assertDoesNotThrow {
            cable.callPowerUpdate()
        }
    }
}
