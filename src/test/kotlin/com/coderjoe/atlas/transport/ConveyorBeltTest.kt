package com.coderjoe.atlas.transport

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callTransportUpdate
import com.coderjoe.atlas.transport.block.ConveyorBelt
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

class ConveyorBeltTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `conveyor belt has correct facing`() {
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, belt.facing)
    }

    @Test
    fun `conveyor belt visual state always returns BLOCK_ID`() {
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(
            "atlas:conveyor_belt", belt.getVisualStateBlockId()
        )
    }

    @Test
    fun `conveyor belt base block ID is atlas conveyor_belt`() {
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("atlas:conveyor_belt", belt.baseBlockId)
    }

    @Test
    fun `conveyor belt descriptor has correct properties`() {
        val desc = ConveyorBelt.descriptor
        assertEquals("atlas:conveyor_belt", desc.baseBlockId)
        assertEquals("Conveyor Belt", desc.displayName)
    }

    @Test
    fun `conveyor belt descriptor has directional placement`() {
        val desc = ConveyorBelt.descriptor
        assertEquals(
            com.coderjoe.atlas.core.PlacementType.DIRECTIONAL,
            desc.placementType
        )
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initTransportFactory()
        assertTrue(
            TransportBlockFactory.isRegistered("atlas:conveyor_belt")
        )
    }

    @Test
    fun `factory creates ConveyorBelt from base ID`() {
        TestHelper.initTransportFactory()
        val block = TransportBlockFactory.createTransportBlock(
            "atlas:conveyor_belt",
            TestHelper.createLocation(),
            BlockFace.NORTH
        )
        assertTrue(block is ConveyorBelt)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `transport update does not throw with no nearby entities`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any()
            )
        } returns emptyList()

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    @Test
    fun `transport update moves item north`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(
            TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH
        )

        val itemLoc =
            Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any()
            )
        } returns listOf(mockItem)

        belt.callTransportUpdate()

        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.z < 0.5 && loc.x == 0.5
                },
            )
        }
    }

    @Test
    fun `transport update moves item east`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(
            TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST
        )

        val itemLoc =
            Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any()
            )
        } returns listOf(mockItem)

        belt.callTransportUpdate()

        verify {
            mockItem.teleportAsync(
                match { loc ->
                    loc.x > 0.5 && loc.z == 0.5
                },
            )
        }
    }

    @Test
    fun `transport update moves multiple items`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(
            TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH
        )

        val item1 = mockk<Item>(relaxed = true)
        val item2 = mockk<Item>(relaxed = true)
        every { item1.location } returns
            Location(TestHelper.mockWorld, 0.3, 64.4, 0.3)
        every { item2.location } returns
            Location(TestHelper.mockWorld, 0.7, 64.4, 0.7)
        every { item1.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)
        every { item2.teleportAsync(any()) } returns
            CompletableFuture.completedFuture(true)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any()
            )
        } returns listOf(item1, item2)

        belt.callTransportUpdate()

        verify { item1.teleportAsync(any()) }
        verify { item2.teleportAsync(any()) }
    }

    @Test
    fun `transport update ignores non-item entities`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        val mockPlayer =
            mockk<org.bukkit.entity.Player>(relaxed = true)
        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any()
            )
        } returns listOf(mockPlayer)

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    @Test
    fun `descriptor description mentions direction`() {
        assertTrue(
            ConveyorBelt.descriptor.description.contains("forward")
        )
    }
}
