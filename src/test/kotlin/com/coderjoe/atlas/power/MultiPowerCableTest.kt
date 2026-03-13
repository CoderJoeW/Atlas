package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.MultiPowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

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
        val cable = MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, cable.facing)
    }

    @Test
    fun `visual state unpowered matches facing`() {
        for ((face, id) in MultiPowerCable.DIRECTIONAL_IDS) {
            val cable = MultiPowerCable(TestHelper.createLocation(), face)
            cable.currentPower = 0
            assertEquals(id, cable.getVisualStateBlockId())
        }
    }

    @Test
    fun `visual state powered matches facing`() {
        for ((face, id) in MultiPowerCable.POWERED_IDS) {
            val cable = MultiPowerCable(TestHelper.createLocation(), face)
            cable.currentPower = 5
            assertEquals(id, cable.getVisualStateBlockId())
        }
    }

    @Test
    fun `base block ID is multi_power_cable`() {
        val cable = MultiPowerCable(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("multi_power_cable", cable.baseBlockId)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = MultiPowerCable.descriptor
        assertEquals("multi_power_cable", desc.baseBlockId)
        assertEquals("Multi Power Cable", desc.displayName)
        assertEquals(12, desc.allRegistrableIds.size)
        for (id in MultiPowerCable.DIRECTIONAL_IDS.values) {
            assertTrue(desc.allRegistrableIds.contains(id), "Missing unpowered ID: $id")
        }
        for (id in MultiPowerCable.POWERED_IDS.values) {
            assertTrue(desc.allRegistrableIds.contains(id), "Missing powered ID: $id")
        }
    }

    @Test
    fun `descriptor has directional placement`() {
        val desc = MultiPowerCable.descriptor
        assertEquals(com.coderjoe.atlas.core.PlacementType.DIRECTIONAL, desc.placementType)
        assertEquals(6, desc.directionalVariants.size)
    }

    @Test
    fun `facingFromBlockId returns correct facing for unpowered IDs`() {
        assertEquals(BlockFace.NORTH, MultiPowerCable.facingFromBlockId("multi_power_cable_north"))
        assertEquals(BlockFace.SOUTH, MultiPowerCable.facingFromBlockId("multi_power_cable_south"))
        assertEquals(BlockFace.EAST, MultiPowerCable.facingFromBlockId("multi_power_cable_east"))
        assertEquals(BlockFace.WEST, MultiPowerCable.facingFromBlockId("multi_power_cable_west"))
        assertEquals(BlockFace.UP, MultiPowerCable.facingFromBlockId("multi_power_cable_up"))
        assertEquals(BlockFace.DOWN, MultiPowerCable.facingFromBlockId("multi_power_cable_down"))
    }

    @Test
    fun `facingFromBlockId returns correct facing for powered IDs`() {
        assertEquals(BlockFace.NORTH, MultiPowerCable.facingFromBlockId("multi_power_cable_north_powered"))
        assertEquals(BlockFace.SOUTH, MultiPowerCable.facingFromBlockId("multi_power_cable_south_powered"))
        assertEquals(BlockFace.EAST, MultiPowerCable.facingFromBlockId("multi_power_cable_east_powered"))
        assertEquals(BlockFace.WEST, MultiPowerCable.facingFromBlockId("multi_power_cable_west_powered"))
        assertEquals(BlockFace.UP, MultiPowerCable.facingFromBlockId("multi_power_cable_up_powered"))
        assertEquals(BlockFace.DOWN, MultiPowerCable.facingFromBlockId("multi_power_cable_down_powered"))
    }

    @Test
    fun `facingFromBlockId returns null for unknown ID`() {
        assertNull(MultiPowerCable.facingFromBlockId("multi_power_cable_diagonal"))
        assertNull(MultiPowerCable.facingFromBlockId("unknown"))
    }

    @Test
    fun `all directional IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in MultiPowerCable.DIRECTIONAL_IDS.values) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing multi power cable ID: $id")
        }
    }

    @Test
    fun `all powered IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in MultiPowerCable.POWERED_IDS.values) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing multi power cable powered ID: $id")
        }
    }

    @Test
    fun `factory creates MultiPowerCable from directional ID`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("multi_power_cable_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is MultiPowerCable)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `max storage is 10`() {
        val cable = MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(10, cable.maxStorage)
    }

    @Test
    fun `pulls power from behind`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, cable, "multi_power_cable_north")

        // Place a battery behind (south, opposite of north-facing)
        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "small_battery")

        cable.callPowerUpdate()

        assertTrue(cable.currentPower > 0, "Cable should have pulled power")
        assertTrue(battery.currentPower < 5, "Battery should have less power")
    }

    @Test
    fun `distributes power to multiple outputs`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        cable.currentPower = 5
        TestHelper.addToRegistry(registry, cable, "multi_power_cable_north")

        // Place batteries on output faces (not behind/south)
        val eastBatteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val eastBattery = SmallBattery(eastBatteryLoc, BlockFace.WEST)
        TestHelper.addToRegistry(registry, eastBattery, "small_battery")

        val westBatteryLoc = TestHelper.createLocation(-1.0, 64.0, 0.0)
        val westBattery = SmallBattery(westBatteryLoc, BlockFace.EAST)
        TestHelper.addToRegistry(registry, westBattery, "small_battery_east")

        val northBatteryLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val northBattery = SmallBattery(northBatteryLoc, BlockFace.SOUTH)
        TestHelper.addToRegistry(registry, northBattery, "small_battery_south")

        cable.callPowerUpdate()

        assertTrue(eastBattery.currentPower > 0, "East battery should have received power")
        assertTrue(westBattery.currentPower > 0, "West battery should have received power")
        assertTrue(northBattery.currentPower > 0, "North battery should have received power")
        assertEquals(2, cable.currentPower, "Cable should have distributed 3 power (1 to each)")
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val cableLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val cable = MultiPowerCable(cableLoc, BlockFace.NORTH)
        cable.currentPower = 10 // Already full
        TestHelper.addToRegistry(registry, cable, "multi_power_cable_north")

        val batteryLoc = TestHelper.createLocation(0.0, 64.0, 1.0)
        val battery = SmallBattery(batteryLoc, BlockFace.NORTH)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "small_battery")

        cable.callPowerUpdate()

        assertEquals(10, cable.maxStorage)
        assertEquals(5, battery.currentPower, "Battery should not lose power when cable is full")
    }

    @Test
    fun `does not crash with no adjacent blocks`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val cable = MultiPowerCable(TestHelper.createLocation(), BlockFace.NORTH)

        assertDoesNotThrow {
            cable.callPowerUpdate()
        }
    }
}
