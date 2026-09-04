package com.coderjoe.atlas.transport

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callTransportUpdate
import com.coderjoe.atlas.transport.block.ConveyorBelt
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
            "atlas:conveyor_belt",
            belt.getVisualStateBlockId(),
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
            desc.placementType,
        )
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initTransportFactory()
        assertTrue(
            TransportBlockFactory.isRegistered("atlas:conveyor_belt"),
        )
    }

    @Test
    fun `factory creates ConveyorBelt from base ID`() {
        TestHelper.initTransportFactory()
        val block =
            TransportBlockFactory.createTransportBlock(
                "atlas:conveyor_belt",
                TestHelper.createLocation(),
                BlockFace.NORTH,
            )
        assertTrue(block is ConveyorBelt)
        assertEquals(BlockFace.NORTH, block!!.facing)
    }

    /** A dropped item sitting at the given spot, optionally still falling. */
    private fun itemAt(
        x: Double,
        y: Double,
        z: Double,
        fallSpeed: Double = 0.0,
    ): Item {
        val item = mockk<Item>(relaxed = true)
        every { item.location } returns Location(TestHelper.mockWorld, x, y, z)
        every { item.velocity } returns Vector(0.0, fallSpeed, 0.0)
        return item
    }

    @Test
    fun `transport update does not throw with no nearby entities`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt =
            ConveyorBelt(TestHelper.createLocation(), BlockFace.NORTH)

        every {
            TestHelper.mockWorld.getNearbyEntities(
                any<Location>(), any(), any(), any(),
            )
        } returns emptyList()

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    @Test
    fun `transport update drives an item along its facing`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val item = itemAt(0.5, 64.375, 0.5)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        // north is -Z, and the belt drives with velocity rather than teleporting
        verify { item.velocity = match { it.z < 0 && it.x == 0.0 } }
    }

    @Test
    fun `transport update drives an item east`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)

        val item = itemAt(0.5, 64.375, 0.5)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        verify { item.velocity = match { it.x > 0 && it.z == 0.0 } }
    }

    @Test
    fun `transport update steers a drifting item back to the centre line`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        // sitting off to the west of the belt's centre line
        val item = itemAt(0.2, 64.375, 0.5)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        // pushed east, back toward x = 0.5, while still travelling north
        verify { item.velocity = match { it.x > 0 && it.z < 0 } }
    }

    @Test
    fun `transport update leaves vertical motion alone`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val item = itemAt(0.5, 64.375, 0.5, fallSpeed = -0.4)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        // an item still falling onto the belt must keep falling, not be pinned in the air
        verify { item.velocity = match { it.y == -0.4 } }
    }

    @Test
    fun `transport update drives every item on the belt`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val first = itemAt(0.5, 64.375, 0.5)
        val second = itemAt(0.5, 64.375, 0.6)
        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns
            listOf(first, second)

        belt.callTransportUpdate()

        verify { first.velocity = any() }
        verify { second.velocity = any() }
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
                any<Location>(), any(), any(), any(),
            )
        } returns listOf(mockPlayer)

        assertDoesNotThrow {
            belt.callTransportUpdate()
        }
    }

    /** A stack of [amount]; real ItemStacks need a Bukkit registry the unit tests do not have. */
    private fun stackOf(amount: Int): ItemStack {
        val stack = mockk<ItemStack>(relaxed = true)
        every { stack.amount } returns amount
        return stack
    }

    /** Wires up the block the belt points at so it reports as [container]. */
    private fun containerAhead(inventory: Inventory): org.bukkit.block.Container {
        val container = mockk<org.bukkit.block.Container>(relaxed = true)
        every { container.inventory } returns inventory
        val ahead = mockk<Block>(relaxed = true)
        every { ahead.state } returns container
        val self = mockk<Block>(relaxed = true)
        every { self.getRelative(any<BlockFace>()) } returns ahead
        every { TestHelper.mockWorld.getBlockAt(any<Location>()) } returns self
        return container
    }

    @Test
    fun `belt deposits into a container it points at`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val stack = stackOf(4)
        val item = itemAt(0.5, 64.375, 0.5)
        every { item.itemStack } returns stack

        val inventory = mockk<Inventory>(relaxed = true)
        every { inventory.addItem(stack) } returns HashMap()
        containerAhead(inventory)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        verify { inventory.addItem(stack) }
        verify { item.remove() }
    }

    @Test
    fun `a full container leaves the item on the belt`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val stack = stackOf(4)
        val item = itemAt(0.5, 64.375, 0.5)
        every { item.itemStack } returns stack

        val inventory = mockk<Inventory>(relaxed = true)
        // nothing fitted - the whole stack comes back
        every { inventory.addItem(stack) } returns hashMapOf(0 to stackOf(4))
        containerAhead(inventory)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        verify(exactly = 0) { item.remove() }
        // still carried, so it can queue up against the container rather than vanishing
        verify { item.velocity = any() }
    }

    @Test
    fun `a partial deposit leaves the remainder on the belt`() {
        TransportBlockRegistry(TestHelper.mockPlugin)
        val belt = ConveyorBelt(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        val stack = stackOf(4)
        val item = itemAt(0.5, 64.375, 0.5)
        every { item.itemStack } returns stack

        val leftover = stackOf(1)
        val inventory = mockk<Inventory>(relaxed = true)
        every { inventory.addItem(stack) } returns hashMapOf(0 to leftover)
        containerAhead(inventory)

        every { TestHelper.mockWorld.getNearbyEntities(any<Location>(), any(), any(), any()) } returns listOf(item)

        belt.callTransportUpdate()

        verify(exactly = 0) { item.remove() }
        verify { item.itemStack = leftover }
    }

    @Test
    fun `descriptor description mentions direction`() {
        assertTrue(
            ConveyorBelt.descriptor.description.contains("forward"),
        )
    }
}
