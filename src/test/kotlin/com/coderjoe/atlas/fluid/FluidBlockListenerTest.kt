package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.core.AtlasBlockListener
import com.coderjoe.atlas.core.BlockSystem
import com.coderjoe.atlas.fluid.block.FluidPump
import io.mockk.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

class FluidBlockListenerTest {

    private lateinit var registry: FluidBlockRegistry
    private lateinit var listener: AtlasBlockListener

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = FluidBlockRegistry(TestHelper.mockPlugin)
        val system = BlockSystem<FluidBlock>(
            name = "fluid",
            registry = registry,
            factory = FluidBlockFactory,
            descriptors = emptyMap(),
            showDialog = { _, _ -> }
        )
        listener = AtlasBlockListener(TestHelper.mockPlugin, listOf(system))
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `onBlockPlace skips when in updatingLocations`() {
        val loc = TestHelper.createLocation()
        val key = FluidBlockRegistry.locationKey(loc)
        registry.updatingLocations.add(key)

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns block

        listener.onBlockPlace(event)
        assertNull(registry.getFluidBlock(loc))
    }

    @Test
    fun `onBlockBreak skips when in updatingLocations`() {
        val loc = TestHelper.createLocation()
        val key = FluidBlockRegistry.locationKey(loc)
        registry.updatingLocations.add(key)

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        val event = mockk<BlockBreakEvent>(relaxed = true)
        every { event.block } returns block

        assertDoesNotThrow { listener.onBlockBreak(event) }
    }

    @Test
    fun `onBlockBreak unregisters fluid block`() {
        val loc = TestHelper.createLocation()
        val pump = FluidPump(loc)
        TestHelper.addToRegistry(registry, pump, "fluid_pump")

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        every { block.world } returns TestHelper.mockWorld
        val event = mockk<BlockBreakEvent>(relaxed = true)
        every { event.block } returns block

        try {
            listener.onBlockBreak(event)
        } catch (_: NoClassDefFoundError) {}
        catch (_: ExceptionInInitializerError) {}

        assertNull(registry.getFluidBlock(loc))
    }

    @Test
    fun `onPlayerInteract only RIGHT_CLICK_BLOCK triggers`() {
        val player = mockk<Player>(relaxed = true)
        val block = mockk<Block>(relaxed = true)
        every { block.location } returns TestHelper.createLocation()

        val event = mockk<PlayerInteractEvent>(relaxed = true)
        every { event.action } returns Action.LEFT_CLICK_BLOCK
        every { event.player } returns player
        every { event.clickedBlock } returns block

        listener.onPlayerInteract(event)
        verify(exactly = 0) { event.isCancelled = true }
    }

    @Test
    fun `onPlayerInteract sneaking does not trigger`() {
        val player = mockk<Player>(relaxed = true)
        every { player.isSneaking } returns true
        val block = mockk<Block>(relaxed = true)
        every { block.location } returns TestHelper.createLocation()

        val event = mockk<PlayerInteractEvent>(relaxed = true)
        every { event.action } returns Action.RIGHT_CLICK_BLOCK
        every { event.player } returns player
        every { event.clickedBlock } returns block

        listener.onPlayerInteract(event)
        verify(exactly = 0) { event.isCancelled = true }
    }

    @Test
    fun `onPlayerInteract ignores non-fluid-block location`() {
        val player = mockk<Player>(relaxed = true)
        every { player.isSneaking } returns false
        val block = mockk<Block>(relaxed = true)
        every { block.location } returns TestHelper.createLocation(99.0, 99.0, 99.0)

        val event = mockk<PlayerInteractEvent>(relaxed = true)
        every { event.action } returns Action.RIGHT_CLICK_BLOCK
        every { event.player } returns player
        every { event.clickedBlock } returns block

        listener.onPlayerInteract(event)
        verify(exactly = 0) { event.isCancelled = true }
    }

    @Test
    fun `getPlayerFacing returns UP when dy positive`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(0.0, 65.0, 0.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.UP, AtlasBlockListener.getPlayerFacing(event))
    }

    @Test
    fun `getPlayerFacing returns EAST when dx positive`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(1.0, 64.0, 0.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.EAST, AtlasBlockListener.getPlayerFacing(event))
    }

    @Test
    fun `getPlayerFacing returns DOWN when dy negative`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(0.0, 63.0, 0.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.DOWN, AtlasBlockListener.getPlayerFacing(event))
    }

    @Test
    fun `getPlayerFacing returns WEST when dx negative`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(-1.0, 64.0, 0.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.WEST, AtlasBlockListener.getPlayerFacing(event))
    }

    @Test
    fun `getPlayerFacing returns SOUTH when dz positive`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(0.0, 64.0, 1.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.SOUTH, AtlasBlockListener.getPlayerFacing(event))
    }

    @Test
    fun `getPlayerFacing returns NORTH when dz negative`() {
        val placed = mockk<Block>(relaxed = true)
        val against = mockk<Block>(relaxed = true)
        every { placed.location } returns TestHelper.createLocation(0.0, 64.0, -1.0)
        every { against.location } returns TestHelper.createLocation(0.0, 64.0, 0.0)

        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against

        assertEquals(BlockFace.NORTH, AtlasBlockListener.getPlayerFacing(event))
    }
}
