package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.PowerSplitter
import com.coderjoe.atlas.power.block.SmallBattery
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerSplitterTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `power splitter has correct facing`() {
        val splitter =
            PowerSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, splitter.facing)
    }

    @Test
    fun `visual state always returns BLOCK_ID`() {
        val splitter =
            PowerSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        splitter.currentPower = 0
        assertEquals(
            "atlas:power_splitter",
            splitter.getVisualStateBlockId(),
        )
        splitter.currentPower = 5
        assertEquals(
            "atlas:power_splitter",
            splitter.getVisualStateBlockId(),
        )
    }

    @Test
    fun `base block ID is atlas power_splitter`() {
        val splitter =
            PowerSplitter(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("atlas:power_splitter", splitter.baseBlockId)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = PowerSplitter.descriptor
        assertEquals("atlas:power_splitter", desc.baseBlockId)
        assertEquals("Power Splitter", desc.displayName)
    }

    @Test
    fun `descriptor has directional placement`() {
        val desc = PowerSplitter.descriptor
        assertEquals(
            com.coderjoe.atlas.core.PlacementType.DIRECTIONAL,
            desc.placementType,
        )
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:power_splitter"),
        )
    }

    @Test
    fun `factory creates PowerSplitter from base ID`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:power_splitter",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is PowerSplitter)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `max storage is 10`() {
        val splitter =
            PowerSplitter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(10, splitter.maxStorage)
    }

    @Test
    fun `pulls power from behind`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = PowerSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            splitter,
            "atlas:power_splitter",
        )

        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        splitter.callPowerUpdate()

        assertTrue(splitter.currentPower > 0)
        assertTrue(battery.currentPower < 5)
    }

    @Test
    fun `distributes power to multiple outputs`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = PowerSplitter(splitterLoc, BlockFace.NORTH)
        splitter.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            splitter,
            "atlas:power_splitter",
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

        splitter.callPowerUpdate()

        assertTrue(eastBattery.currentPower > 0)
        assertTrue(westBattery.currentPower > 0)
        assertTrue(northBattery.currentPower > 0)
        assertEquals(2, splitter.currentPower)
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = PowerSplitter(splitterLoc, BlockFace.NORTH)
        splitter.currentPower = 10
        TestHelper.addToRegistry(
            registry,
            splitter,
            "atlas:power_splitter",
        )

        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        splitter.callPowerUpdate()

        assertEquals(10, splitter.maxStorage)
        assertEquals(5, battery.currentPower)
    }

    @Test
    fun `does not crash with no adjacent blocks`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val splitter =
            PowerSplitter(TestHelper.createLocation(), BlockFace.NORTH)

        assertDoesNotThrow {
            splitter.callPowerUpdate()
        }
    }

    @Test
    fun `distributes power round-robin across multiple updates`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)

        // Splitter facing NORTH, input from SOUTH
        val splitterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val splitter = PowerSplitter(splitterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, splitter, "atlas:power_splitter")

        // Two output batteries: east and west (both can accept power)
        val eastLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val eastBattery = SmallBattery(eastLoc, BlockFace.WEST)
        TestHelper.addToRegistry(registry, eastBattery, "atlas:small_battery")

        val westLoc = TestHelper.createLocation(-1.0, 64.0, 0.0)
        val westBattery = SmallBattery(westLoc, BlockFace.EAST)
        TestHelper.addToRegistry(registry, westBattery, "atlas:small_battery")

        // Give splitter exactly 1 power and update — only one battery should get it
        splitter.currentPower = 1
        splitter.callPowerUpdate()

        val firstEast = eastBattery.currentPower
        val firstWest = westBattery.currentPower
        assertEquals(1, firstEast + firstWest, "exactly 1 power distributed")

        // Reset and do it again — the OTHER battery should get power this time
        splitter.currentPower = 1
        eastBattery.currentPower = 0
        westBattery.currentPower = 0
        splitter.callPowerUpdate()

        val secondEast = eastBattery.currentPower
        val secondWest = westBattery.currentPower
        assertEquals(1, secondEast + secondWest, "exactly 1 power distributed")

        // The two rounds should have gone to different targets
        val firstWentEast = firstEast == 1
        val secondWentEast = secondEast == 1
        assertTrue(
            firstWentEast != secondWentEast,
            "round-robin should alternate: first=$firstWentEast, second=$secondWentEast",
        )
    }
}
