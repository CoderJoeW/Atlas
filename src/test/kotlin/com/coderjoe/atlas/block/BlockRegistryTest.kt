package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.transport.ConveyorBelt
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * One index for every block, whatever system it belongs to.
 *
 * Merged from the two per-system registry test files, which tested the same class through the
 * same methods once the three registries collapsed into this one.
 */
class BlockRegistryTest {
    private lateinit var registry: BlockRegistry

    @BeforeEach
    fun setup() {
        MockServer.setup()
        registry = BlockRegistry(MockServer.plugin)
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `locationKey produces correct format`() {
        assertEquals("world:10,64,-5", BlockRegistry.locationKey(MockServer.createLocation(10.0, 64.0, -5.0)))
        assertEquals("world:5,100,-3", BlockRegistry.locationKey(MockServer.createLocation(5.0, 100.0, -3.0)))
    }

    @Test
    fun `a tracked block reaches its neighbours through its own context`() {
        val cable = PowerCable(MockServer.createLocation())
        val belt = ConveyorBelt(MockServer.createLocation(1.0), BlockFace.NORTH)
        registry.track(cable, PowerCable.BLOCK_ID)
        registry.track(belt, ConveyorBelt.BLOCK_ID)

        assertSame(belt, cable.neighbor(BlockFace.EAST))
        assertNull(cable.neighbor(BlockFace.WEST))
    }

    @Test
    fun `an unregistered block says so instead of reaching for a global`() {
        val cable = PowerCable(MockServer.createLocation())

        val error = assertThrows<IllegalStateException> { cable.neighbor(BlockFace.EAST) }
        assertTrue(error.message!!.contains("is not registered"))
    }

    @Test
    fun `register and get returns block`() {
        val loc = MockServer.createLocation()
        val block = SmallSolarPanel(loc)
        registry.track(block, SmallSolarPanel.BLOCK_ID)

        assertSame(block, registry.getBlock(loc))
    }

    @Test
    fun `unregister removes and returns block`() {
        val loc = MockServer.createLocation()
        val pump = FluidPump(loc)
        registry.track(pump, FluidPump.BLOCK_ID)

        val removed = registry.unregister(loc)

        assertSame(pump, removed)
        assertNull(registry.getBlock(loc))
    }

    @Test
    fun `unregister non-existent location returns null`() {
        assertNull(registry.unregister(MockServer.createLocation(99.0, 99.0, 99.0)))
    }

    @Test
    fun `getAdjacentBlock returns block in correct direction`() {
        val northBlock = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(northBlock, SmallSolarPanel.BLOCK_ID)

        val adjacent = registry.getAdjacentBlock(MockServer.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        assertSame(northBlock, adjacent)
    }

    @Test
    fun `getAdjacentBlock returns null when no block in direction`() {
        assertNull(registry.getAdjacentBlock(MockServer.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH))
    }

    @Test
    fun `getAdjacentBlocks returns blocks in all 6 directions`() {
        val offsets =
            listOf(
                Triple(1.0, 0.0, 0.0),
                Triple(-1.0, 0.0, 0.0),
                Triple(0.0, 1.0, 0.0),
                Triple(0.0, -1.0, 0.0),
                Triple(0.0, 0.0, 1.0),
                Triple(0.0, 0.0, -1.0),
            )
        for ((dx, dy, dz) in offsets) {
            val neighborLoc = MockServer.createLocation(dx, 64.0 + dy, dz)
            registry.track(SmallSolarPanel(neighborLoc), SmallSolarPanel.BLOCK_ID)
        }

        val adjacent = registry.getAdjacentBlocks(MockServer.createLocation(0.0, 64.0, 0.0))

        assertEquals(6, adjacent.size)
    }

    @Test
    fun `getAdjacentBlocks returns empty when no neighbors`() {
        assertEquals(0, registry.getAdjacentBlocks(MockServer.createLocation(0.0, 64.0, 0.0)).size)
    }

    @Test
    fun `getAllBlocksWithIds returns correct pairs`() {
        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0))
        val battery = SmallBattery(MockServer.createLocation(1.0, 64.0, 0.0))
        val pipe = FluidPipe(MockServer.createLocation(2.0, 64.0, 0.0))
        registry.track(panel, SmallSolarPanel.BLOCK_ID)
        registry.track(battery, SmallBattery.BLOCK_ID)
        registry.track(pipe, FluidPipe.BLOCK_ID)

        val pairs = registry.getAllBlocksWithIds()

        assertEquals(3, pairs.size)
        assertTrue(pairs.any { it.first === panel && it.second == SmallSolarPanel.BLOCK_ID })
        assertTrue(pairs.any { it.first === battery && it.second == SmallBattery.BLOCK_ID })
        assertTrue(pairs.any { it.first === pipe && it.second == FluidPipe.BLOCK_ID })
    }

    @Test
    fun `getAllBlocks returns all registered blocks`() {
        registry.track(SmallSolarPanel(MockServer.createLocation(0.0, 64.0, 0.0)), SmallSolarPanel.BLOCK_ID)
        registry.track(SmallSolarPanel(MockServer.createLocation(1.0, 64.0, 0.0)), SmallSolarPanel.BLOCK_ID)

        assertEquals(2, registry.getAllBlocks().size)
    }

    @Test
    fun `stopAll clears all blocks`() {
        registry.track(SmallSolarPanel(MockServer.createLocation()), SmallSolarPanel.BLOCK_ID)
        registry.track(FluidPump(MockServer.createLocation(1.0, 64.0, 0.0)), FluidPump.BLOCK_ID)

        registry.stopAll()

        assertEquals(0, registry.getAllBlocks().size)
        assertEquals(0, registry.getAllBlocksWithIds().size)
    }

    /** The one case where a single index could silently drop a block of a different family. */
    @Test
    fun `registering at same location overwrites`() {
        val loc = MockServer.createLocation()
        val panel = SmallSolarPanel(loc)
        val pump = FluidPump(loc)
        registry.track(panel, SmallSolarPanel.BLOCK_ID)
        registry.track(pump, FluidPump.BLOCK_ID)

        assertSame(pump, registry.getBlock(loc))
        assertEquals(1, registry.getAllBlocks().size)
    }

    @Test
    fun `blocks of different families share one index`() {
        val cable = PowerCable(MockServer.createLocation())
        val belt = ConveyorBelt(MockServer.createLocation(1.0), BlockFace.NORTH)
        registry.track(cable, PowerCable.BLOCK_ID)
        registry.track(belt, ConveyorBelt.BLOCK_ID)

        assertEquals(2, registry.getAllBlocks().size)
        assertSame(belt, registry.getAdjacentBlock(cable.location, BlockFace.EAST))
    }
}
