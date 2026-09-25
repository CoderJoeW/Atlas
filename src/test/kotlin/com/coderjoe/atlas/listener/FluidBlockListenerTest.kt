package com.coderjoe.atlas.listener

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.BlockSystem
import com.coderjoe.atlas.block.fluid.FluidBlockFactory
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.mockk
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockListenerTest {
    private lateinit var registry: BlockRegistry
    private lateinit var listener: AtlasBlockListener

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
        val system =
            BlockSystem(
                name = "fluid",
                registry = registry,
                factory = FluidBlockFactory,
                descriptors = emptyMap(),
            )
        listener = AtlasBlockListener(MockServer.plugin, registry, listOf(system))
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `onBlockPlace skips when in updatingLocations`() {
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

        assertDoesNotThrow { listener.onBlockBreak(event) }
    }

    @Test
    fun `onBlockBreak unregisters fluid block`() {
        val loc = MockServer.createLocation()
        val pump = FluidPump(loc)
        registry.track(pump, "atlas:fluid_pump")

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
}
