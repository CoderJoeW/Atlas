package com.coderjoe.atlas.listener

import com.coderjoe.atlas.AtlasBlockTypes
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockListenerTest {
    private lateinit var registry: BlockRegistry
    private lateinit var listener: AtlasBlockListener

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
        listener = AtlasBlockListener(MockServer.plugin, registry, AtlasBlockTypes.catalog)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `onBlockPlace skips when location in updatingLocations`() {
        val loc = MockServer.createLocation()
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
        val loc = MockServer.createLocation()
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
        val loc = MockServer.createLocation()
        val panel = SmallSolarPanel(loc)
        registry.track(
            panel,
            "atlas:small_solar_panel",
        )

        val block = mockk<Block>(relaxed = true)
        every { block.location } returns loc
        every { block.world } returns MockServer.world
        val event = mockk<BlockBreakEvent>(relaxed = true)
        every { event.block } returns block

        try {
            listener.onBlockBreak(event)
        } catch (_: NoClassDefFoundError) {
        } catch (_: ExceptionInInitializerError) {
        }

        assertNull(registry.getBlock(loc))
    }

    private fun placeEvent(blockId: String?): BlockPlaceEvent {
        mockkObject(CraftEngineHelper)
        every { CraftEngineHelper.getBlockId(any()) } returns blockId
        val placed = mockk<Block>(relaxed = true)
        every { placed.location } returns MockServer.createLocation()
        val against = mockk<Block>(relaxed = true)
        every { against.location } returns MockServer.createLocation(y = 63.0)
        val event = mockk<BlockPlaceEvent>(relaxed = true)
        every { event.block } returns placed
        every { event.blockAgainst } returns against
        return event
    }

    @Test
    fun `placing a battery variant registers a battery under its base id`() {
        listener.onBlockPlace(placeEvent(SmallBattery.BLOCK_ID_LOW))

        val (block, blockId) = registry.getAllBlocksWithIds().single()
        assertInstanceOf(SmallBattery::class.java, block)
        assertEquals(SmallBattery.BLOCK_ID, blockId)
    }

    @Test
    fun `placing a block the catalog does not know registers nothing`() {
        listener.onBlockPlace(placeEvent("minecraft:stone"))

        assertTrue(registry.getAllBlocks().isEmpty())
    }
}
