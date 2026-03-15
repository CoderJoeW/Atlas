package com.coderjoe.atlas.transport

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callTransportUpdate
import com.coderjoe.atlas.transport.block.ConveyorBelt
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
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
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(BlockFace.NORTH, belt.facing)
    }

    @Test
    fun `conveyor belt visual state matches facing`() {
        for ((face, id) in ConveyorBelt.DIRECTIONAL_IDS) {
            val belt = ConveyorBelt(TestHelper.createLocation(), face)
            assertEquals(id, belt.getVisualStateBlockId())
        }
    }

    @Test
    fun `conveyor belt base block ID is conveyor_belt`() {
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.SOUTH)
        assertEquals("conveyor_belt", belt.baseBlockId)
    }

    @Test
    fun `conveyor belt descriptor has correct properties`() {
        val desc = ConveyorBelt.descriptor
        assertEquals("conveyor_belt", desc.baseBlockId)
        assertEquals("Conveyor Belt", desc.displayName)
        assertEquals(4, desc.allRegistrableIds.size)
        assertTrue(desc.allRegistrableIds.contains("conveyor_belt_north"))
        assertTrue(desc.allRegistrableIds.contains("conveyor_belt_south"))
        assertTrue(desc.allRegistrableIds.contains("conveyor_belt_east"))
        assertTrue(desc.allRegistrableIds.contains("conveyor_belt_west"))
    }

    @Test
    fun `conveyor belt descriptor has directional placement`() {
        val desc = ConveyorBelt.descriptor
        assertEquals(com.coderjoe.atlas.core.PlacementType.DIRECTIONAL, desc.placementType)
        assertEquals(4, desc.directionalVariants.size)
    }

    @Test
    fun `facingFromBlockId returns correct facing`() {
        assertEquals(BlockFace.NORTH, ConveyorBelt.facingFromBlockId("conveyor_belt_north"))
        assertEquals(BlockFace.SOUTH, ConveyorBelt.facingFromBlockId("conveyor_belt_south"))
        assertEquals(BlockFace.EAST, ConveyorBelt.facingFromBlockId("conveyor_belt_east"))
        assertEquals(BlockFace.WEST, ConveyorBelt.facingFromBlockId("conveyor_belt_west"))
    }

    @Test
    fun `facingFromBlockId returns null for unknown ID`() {
        assertNull(ConveyorBelt.facingFromBlockId("conveyor_belt_up"))
        assertNull(ConveyorBelt.facingFromBlockId("unknown"))
    }

    @Test
    fun `all directional IDs are registered`() {
        TestHelper.initTransportFactory()
        for (id in ConveyorBelt.DIRECTIONAL_IDS.values) {
            assertTrue(TransportBlockFactory.isRegistered(id), "Missing conveyor belt ID: $id")
        }
    }

    @Test
    fun `factory creates ConveyorBelt from directional ID`() {
        TestHelper.initTransportFactory()
        val block = TransportBlockFactory.createTransportBlock("conveyor_belt_north", TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(block is ConveyorBelt)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    @Test
    fun `transport update does not throw with no nearby entities`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns emptyList()

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    @Test
    fun `transport update moves item north`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockItem)

        belt.callTransportUpdate()

        verify { mockItem.teleportAsync(match { loc ->
            loc.z < 0.5 && loc.x == 0.5
        }) }
    }

    @Test
    fun `transport update moves item east`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)

        val itemLoc = Location(TestHelper.mockWorld, 0.5, 64.375, 0.5)
        val mockItem = mockk<Item>(relaxed = true)
        every { mockItem.location } returns itemLoc
        every { mockItem.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockItem)

        belt.callTransportUpdate()

        verify { mockItem.teleportAsync(match { loc ->
            loc.x > 0.5 && loc.z == 0.5
        }) }
    }

    @Test
    fun `transport update moves multiple items`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)

        val item1 = mockk<Item>(relaxed = true)
        val item2 = mockk<Item>(relaxed = true)
        every { item1.location } returns Location(TestHelper.mockWorld, 0.3, 64.4, 0.3)
        every { item2.location } returns Location(TestHelper.mockWorld, 0.7, 64.4, 0.7)
        every { item1.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)
        every { item2.teleportAsync(any()) } returns CompletableFuture.completedFuture(true)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item1, item2)

        belt.callTransportUpdate()

        verify { item1.teleportAsync(any()) }
        verify { item2.teleportAsync(any()) }
    }

    @Test
    fun `transport update ignores non-item entities`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        val mockPlayer = mockk<org.bukkit.entity.Player>(relaxed = true)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(mockPlayer)

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    @Test
    fun `descriptor description mentions direction`() {
        assertTrue(ConveyorBelt.descriptor.description.contains("forward"))
    }
}
