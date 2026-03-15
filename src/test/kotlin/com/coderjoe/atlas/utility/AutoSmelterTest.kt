package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.utility.block.AutoSmelter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
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
        val smelter =
            AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, smelter.facing)
    }

    @Test
    fun `auto smelter visual state always returns BLOCK_ID`() {
        val smelter =
            AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)
        smelter.currentPower = 0
        assertEquals(
            "atlas:auto_smelter",
            smelter.getVisualStateBlockId(),
        )
        smelter.currentPower = 2
        assertEquals(
            "atlas:auto_smelter",
            smelter.getVisualStateBlockId(),
        )
    }

    @Test
    fun `auto smelter base block ID is atlas auto_smelter`() {
        val smelter =
            AutoSmelter(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("atlas:auto_smelter", smelter.baseBlockId)
    }

    @Test
    fun `auto smelter descriptor has correct properties`() {
        val desc = AutoSmelter.descriptor
        assertEquals("atlas:auto_smelter", desc.baseBlockId)
        assertEquals("Auto Smelter", desc.displayName)
    }

    @Test
    fun `auto smelter descriptor has directional placement`() {
        val desc = AutoSmelter.descriptor
        assertEquals(
            com.coderjoe.atlas.core.PlacementType.DIRECTIONAL,
            desc.placementType,
        )
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(
            PowerBlockFactory.isRegistered("atlas:auto_smelter"),
        )
    }

    @Test
    fun `factory creates AutoSmelter from base ID`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:auto_smelter",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is AutoSmelter)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `max storage is 2`() {
        val smelter =
            AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(2, smelter.maxStorage)
    }

    @Test
    fun `power update does not throw with no nearby entities`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter =
            AutoSmelter(TestHelper.createLocation(), BlockFace.NORTH)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        assertDoesNotThrow {
            smelter.callPowerUpdate()
        }
    }

    @Test
    fun `power update moves item forward without smelting when no power`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter =
            AutoSmelter(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.NORTH,
            )
        smelter.currentPower = 0

        val itemLoc =
            Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

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
        val smelter =
            AutoSmelter(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.EAST,
            )
        smelter.currentPower = 0

        val itemLoc =
            Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

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
    fun `items still move forward when powered but no smelting recipe`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val smelter =
            AutoSmelter(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.NORTH,
            )
        smelter.currentPower = 2

        val itemLoc =
            Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockItem)

        smelter.callPowerUpdate()

        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.z < 0.5 && loc.x == 0.5
                },
            )
        }
        assertEquals(2, smelter.currentPower)
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val smelterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val smelter = AutoSmelter(smelterLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(
            registry,
            smelter,
            "atlas:auto_smelter",
        )

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery =
            com.coderjoe.atlas.power.block.SmallBattery(
                batteryLoc,
                BlockFace.WEST,
            )
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        smelter.callPowerUpdate()

        assertTrue(smelter.currentPower > 0)
        assertTrue(battery.currentPower < 5)
    }

    @Test
    fun `does not exceed max storage when pulling power`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val smelterLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val smelter = AutoSmelter(smelterLoc, BlockFace.NORTH)
        smelter.currentPower = 2
        TestHelper.addToRegistry(
            registry,
            smelter,
            "atlas:auto_smelter",
        )

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery =
            com.coderjoe.atlas.power.block.SmallBattery(
                batteryLoc,
                BlockFace.WEST,
            )
        battery.currentPower = 5
        TestHelper.addToRegistry(
            registry,
            battery,
            "atlas:small_battery",
        )

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        smelter.callPowerUpdate()

        assertEquals(2, smelter.currentPower)
        assertEquals(5, battery.currentPower)
    }
}
