package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.AutoSmelter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class AutoSmelterTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `auto smelter has correct facing`() {
        val smelter = AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, smelter.facing)
    }

    @Test
    fun `auto smelter visual state unpowered matches facing`() {
        for ((face, id) in AutoSmelter.DIRECTIONAL_IDS) {
            val smelter = AutoSmelter(TestHelper.createLocation(), face)
            smelter.currentPower = 0
            assertEquals(id, smelter.getVisualStateBlockId())
        }
    }

    @Test
    fun `auto smelter visual state powered matches facing`() {
        for ((face, id) in AutoSmelter.POWERED_IDS) {
            val smelter = AutoSmelter(TestHelper.createLocation(), face)
            smelter.currentPower = 2
            assertEquals(id, smelter.getVisualStateBlockId())
        }
    }

    @Test
    fun `auto smelter base block ID is auto_smelter`() {
        val smelter = AutoSmelter(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("auto_smelter", smelter.baseBlockId)
    }

    @Test
    fun `auto smelter descriptor has correct properties`() {
        val desc = AutoSmelter.descriptor
        assertEquals("auto_smelter", desc.baseBlockId)
        assertEquals("Auto Smelter", desc.displayName)
        assertEquals(8, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_north"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_south"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_east"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_west"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_north_on"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_south_on"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_east_on"))
        assertTrue(desc.allRegistrableIds.contains("auto_smelter_west_on"))
    }

    @Test
    fun `auto smelter descriptor has directional placement`() {
        val desc = AutoSmelter.descriptor
        assertEquals(com.coderjoe.atlas.core.PlacementType.DIRECTIONAL, desc.placementType)
        assertEquals(4, desc.directionalVariants.size)
    }

    @Test
    fun `facingFromBlockId returns correct facing`() {
        assertEquals(BlockFace.NORTH, AutoSmelter.facingFromBlockId("auto_smelter_north"))
        assertEquals(BlockFace.SOUTH, AutoSmelter.facingFromBlockId("auto_smelter_south"))
        assertEquals(BlockFace.EAST, AutoSmelter.facingFromBlockId("auto_smelter_east"))
        assertEquals(BlockFace.WEST, AutoSmelter.facingFromBlockId("auto_smelter_west"))
    }

    @Test
    fun `facingFromBlockId returns correct facing for powered IDs`() {
        assertEquals(BlockFace.NORTH, AutoSmelter.facingFromBlockId("auto_smelter_north_on"))
        assertEquals(BlockFace.SOUTH, AutoSmelter.facingFromBlockId("auto_smelter_south_on"))
        assertEquals(BlockFace.EAST, AutoSmelter.facingFromBlockId("auto_smelter_east_on"))
        assertEquals(BlockFace.WEST, AutoSmelter.facingFromBlockId("auto_smelter_west_on"))
    }

    @Test
    fun `facingFromBlockId returns null for unknown ID`() {
        assertNull(AutoSmelter.facingFromBlockId("auto_smelter_up"))
        assertNull(AutoSmelter.facingFromBlockId("unknown"))
    }

    @Test
    fun `all directional IDs are registered`() {
        TestHelper.initPowerFactory()
        for (id in AutoSmelter.DIRECTIONAL_IDS.values) {
            assertTrue(PowerBlockFactory.isRegistered(id), "Missing auto smelter ID: $id")
        }
    }

    @Test
    fun `factory creates AutoSmelter from directional ID`() {
        TestHelper.initPowerFactory()
        val block = PowerBlockFactory.createPowerBlock("auto_smelter_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is AutoSmelter)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `max storage is 2`() {
        val smelter = AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(2, smelter.maxStorage)
    }

    @Test
    fun `power update does not throw with no nearby entities`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter = AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns emptyList()

        assertDoesNotThrow {
            smelter.callPowerUpdate()
        }
    }

    @Test
    fun `power update moves item forward without smelting when no power`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter = AutoSmelter(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        smelter.currentPower = 0

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockItem)

        smelter.callPowerUpdate()

        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.z < 0.5 && loc.x == 0.5
                },
            )
        }
    }

    @Test
    fun `power update moves item east`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter = AutoSmelter(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)
        smelter.currentPower = 0

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockItem)

        smelter.callPowerUpdate()

        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.x > 0.5 && loc.z == 0.5
                },
            )
        }
    }

    @Test
    fun `items still move forward when powered but no smelting recipe available`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter = AutoSmelter(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        smelter.currentPower = 2

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)
        // itemStack getter not mocked, so getSmeltingResult will catch the exception and return null

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockItem)

        smelter.callPowerUpdate()

        // Item should still be moved forward even without smelting
        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.z < 0.5 && loc.x == 0.5
                },
            )
        }
        // Power should not be consumed since no recipe was found
        assertEquals(2, smelter.currentPower)
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val smelterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val smelter = AutoSmelter(smelterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, smelter, "auto_smelter_north")

        // Place a battery with power adjacent
        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.WEST)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "small_battery")

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns emptyList()

        smelter.callPowerUpdate()

        assertTrue(smelter.currentPower > 0, "Smelter should have pulled power")
        assertTrue(battery.currentPower < 5, "Battery should have less power")
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val smelterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val smelter = AutoSmelter(smelterLoc, BlockFace.NORTH)
        smelter.currentPower = 2 // Already full
        TestHelper.addToRegistry(registry, smelter, "auto_smelter_north")

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.WEST)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "small_battery")

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns emptyList()

        smelter.callPowerUpdate()

        assertEquals(2, smelter.currentPower, "Should not exceed max storage")
        assertEquals(5, battery.currentPower, "Battery should not lose power when smelter is full")
    }
}
