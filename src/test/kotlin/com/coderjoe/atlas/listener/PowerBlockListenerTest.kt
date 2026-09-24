package com.coderjoe.atlas.listener

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.BlockSystem
import com.coderjoe.atlas.block.power.PowerBlockFactory
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.testing.TestHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockListenerTest {
    private lateinit var registry: BlockRegistry
    private lateinit var listener: AtlasBlockListener

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = BlockRegistry(TestHelper.mockPlugin)
        val system =
            BlockSystem(
                name = "power",
                registry = registry,
                factory = PowerBlockFactory,
                descriptors = emptyMap(),
            )
        listener =
            AtlasBlockListener(TestHelper.mockPlugin, registry, listOf(system)) { _, _ -> }
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `onBlockPlace skips when location in updatingLocations`() {
        val loc = TestHelper.createLocation()
        val key = BlockRegistry.locationKey(loc)
        registry.updatingLocations.add(key)

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns block

        listener.onBlockPlace(event)
        assertNull(registry.getBlock(loc))
    }

    @Test
    fun `onBlockBreak skips when in updatingLocations`() {
        val loc = TestHelper.createLocation()
        val key = BlockRegistry.locationKey(loc)
        registry.updatingLocations.add(key)

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        val event = mockk<BlockBreakEvent>(relaxed = true)
        every { event.block } returns block

        listener.onBlockBreak(event)
    }

    @Test
    fun `onBlockBreak unregisters power block`() {
        val loc = TestHelper.createLocation()
        val panel = SmallSolarPanel(loc)
        TestHelper.addToRegistry(
            registry,
            panel,
            "atlas:small_solar_panel",
        )

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        every { block.world } returns TestHelper.mockWorld
        val event = mockk<BlockBreakEvent>(relaxed = true)
        every { event.block } returns block

        try {
            listener.onBlockBreak(event)
        } catch (_: NoClassDefFoundError) {
        } catch (_: ExceptionInInitializerError) {
        }

        assertNull(registry.getBlock(loc))
    }

    @Test
    fun `onPlayerInteract only triggers on RIGHT_CLICK_BLOCK`() {
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
    fun `onPlayerInteract does not trigger when sneaking`() {
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
    fun `onPlayerInteract ignores non-power-block location`() {
        val player = mockk<Player>(relaxed = true)
        every { player.isSneaking } returns false
        val block = mockk<Block>(relaxed = true)
        every { block.location } returns
            TestHelper.createLocation(
                99.0, 99.0, 99.0,
            )

        val event = mockk<PlayerInteractEvent>(relaxed = true)
        every { event.action } returns Action.RIGHT_CLICK_BLOCK
        every { event.player } returns player
        every { event.clickedBlock } returns block

        listener.onPlayerInteract(event)
        verify(exactly = 0) { event.isCancelled = true }
    }
}
