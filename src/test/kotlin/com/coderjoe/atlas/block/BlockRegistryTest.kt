package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.fluid.block.FluidPipe
import com.coderjoe.atlas.block.fluid.block.FluidPump
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallBattery
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.block.transport.block.ConveyorBelt
import com.coderjoe.atlas.testing.TestHelper
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        TestHelper.setup()
        registry = BlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `locationKey produces correct format`() {
        assertEquals("world:10,64,-5", BlockRegistry.locationKey(TestHelper.createLocation(10.0, 64.0, -5.0)))
        assertEquals("world:5,100,-3", BlockRegistry.locationKey(TestHelper.createLocation(5.0, 100.0, -3.0)))
    }

    @Test
    fun `active is set on creation`() {
        assertSame(registry, BlockRegistry.active)
    }

    @Test
    fun `register and get returns block`() {
        val loc = TestHelper.createLocation()
        val block = SmallSolarPanel(loc)
        TestHelper.addToRegistry(registry, block, SmallSolarPanel.BLOCK_ID)

        assertSame(block, registry.getBlock(loc))
    }

    @Test
    fun `unregister removes and returns block`() {
        val loc = TestHelper.createLocation()
        val pump = FluidPump(loc)
        TestHelper.addToRegistry(registry, pump, FluidPump.BLOCK_ID)

        val removed = registry.unregister(loc)

        assertSame(pump, removed)
        assertNull(registry.getBlock(loc))
    }

    @Test
    fun `unregister non-existent location returns null`() {
        assertNull(registry.unregister(TestHelper.createLocation(99.0, 99.0, 99.0)))
    }

    @Test
    fun `getAdjacentBlock returns block in correct direction`() {
        val northBlock = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, -1.0))
        TestHelper.addToRegistry(registry, northBlock, SmallSolarPanel.BLOCK_ID)

        val adjacent = registry.getAdjacentBlock(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)

        assertSame(northBlock, adjacent)
    }

    @Test
    fun `getAdjacentBlock returns null when no block in direction`() {
        assertNull(registry.getAdjacentBlock(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH))
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
            val neighborLoc = TestHelper.createLocation(dx, 64.0 + dy, dz)
            TestHelper.addToRegistry(registry, SmallSolarPanel(neighborLoc), SmallSolarPanel.BLOCK_ID)
        }

        val adjacent = registry.getAdjacentBlocks(TestHelper.createLocation(0.0, 64.0, 0.0))

        assertEquals(6, adjacent.size)
    }

    @Test
    fun `getAdjacentBlocks returns empty when no neighbors`() {
        assertEquals(0, registry.getAdjacentBlocks(TestHelper.createLocation(0.0, 64.0, 0.0)).size)
    }

    @Test
    fun `getAllBlocksWithIds returns correct pairs`() {
        val panel = SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0))
        val battery = SmallBattery(TestHelper.createLocation(1.0, 64.0, 0.0))
        val pipe = FluidPipe(TestHelper.createLocation(2.0, 64.0, 0.0))
        TestHelper.addToRegistry(registry, panel, SmallSolarPanel.BLOCK_ID)
        TestHelper.addToRegistry(registry, battery, SmallBattery.BLOCK_ID)
        TestHelper.addToRegistry(registry, pipe, FluidPipe.BLOCK_ID)

        val pairs = registry.getAllBlocksWithIds()

        assertEquals(3, pairs.size)
        assertTrue(pairs.any { it.first === panel && it.second == SmallSolarPanel.BLOCK_ID })
        assertTrue(pairs.any { it.first === battery && it.second == SmallBattery.BLOCK_ID })
        assertTrue(pairs.any { it.first === pipe && it.second == FluidPipe.BLOCK_ID })
    }

    @Test
    fun `getAllBlocks returns all registered blocks`() {
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0)), SmallSolarPanel.BLOCK_ID)
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0)), SmallSolarPanel.BLOCK_ID)

        assertEquals(2, registry.getAllBlocks().size)
    }

    @Test
    fun `stopAll clears all blocks`() {
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation()), SmallSolarPanel.BLOCK_ID)
        TestHelper.addToRegistry(registry, FluidPump(TestHelper.createLocation(1.0, 64.0, 0.0)), FluidPump.BLOCK_ID)

        registry.stopAll()

        assertEquals(0, registry.getAllBlocks().size)
        assertEquals(0, registry.getAllBlocksWithIds().size)
    }

    /** The one case where a single index could silently drop a block of a different family. */
    @Test
    fun `registering at same location overwrites`() {
        val loc = TestHelper.createLocation()
        val panel = SmallSolarPanel(loc)
        val pump = FluidPump(loc)
        TestHelper.addToRegistry(registry, panel, SmallSolarPanel.BLOCK_ID)
        TestHelper.addToRegistry(registry, pump, FluidPump.BLOCK_ID)

        assertSame(pump, registry.getBlock(loc))
        assertEquals(1, registry.getAllBlocks().size)
    }

    @Test
    fun `blocks of different families share one index`() {
        val cable = PowerCable(TestHelper.createLocation())
        val belt = ConveyorBelt(TestHelper.createLocation(1.0), BlockFace.NORTH)
        TestHelper.addToRegistry(registry, cable, PowerCable.BLOCK_ID)
        TestHelper.addToRegistry(registry, belt, ConveyorBelt.BLOCK_ID)

        assertEquals(2, registry.getAllBlocks().size)
        assertSame(belt, registry.getAdjacentBlock(cable.location, BlockFace.EAST))
    }

    /** The guard the three registries used to give for free: a cable must not treat a belt as power. */
    @Test
    fun `adjacentOf ignores a neighbour from another family`() {
        val cable = PowerCable(TestHelper.createLocation())
        val belt = ConveyorBelt(TestHelper.createLocation(1.0), BlockFace.NORTH)
        TestHelper.addToRegistry(registry, cable, PowerCable.BLOCK_ID)
        TestHelper.addToRegistry(registry, belt, ConveyorBelt.BLOCK_ID)

        assertNull(registry.adjacentOf<PowerBlock>(cable.location, BlockFace.EAST))
        assertNotNull(registry.getAdjacentBlock(cable.location, BlockFace.EAST))
    }
}
