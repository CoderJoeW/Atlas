package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PowerBlockRegistryTest {
    private lateinit var registry: PowerBlockRegistry

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = PowerBlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `locationKey produces correct format`() {
        val loc = TestHelper.createLocation(10.0, 64.0, -5.0)
        val key = PowerBlockRegistry.locationKey(loc)
        assertEquals("world:10,64,-5", key)
    }

    @Test
    fun `register and get returns block`() {
        val loc = TestHelper.createLocation()
        val block = SmallSolarPanel(loc)
        TestHelper.addToRegistry(registry, block, "small_solar_panel")

        val retrieved = registry.getPowerBlock(loc)
        assertSame(block, retrieved)
    }

    @Test
    fun `unregisterPowerBlock removes and returns block`() {
        val loc = TestHelper.createLocation()
        val block = SmallSolarPanel(loc)
        TestHelper.addToRegistry(registry, block, "small_solar_panel")

        val removed = registry.unregisterPowerBlock(loc)
        assertSame(block, removed)
        assertNull(registry.getPowerBlock(loc))
    }

    @Test
    fun `unregister non-existent location returns null`() {
        val result = registry.unregisterPowerBlock(TestHelper.createLocation(99.0, 99.0, 99.0))
        assertNull(result)
    }

    @Test
    fun `getAdjacentPowerBlock returns block in correct direction`() {
        val loc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val northLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val northBlock = SmallSolarPanel(northLoc)
        TestHelper.addToRegistry(registry, northBlock, "small_solar_panel")

        val adjacent = registry.getAdjacentPowerBlock(loc, BlockFace.NORTH)
        assertSame(northBlock, adjacent)
    }

    @Test
    fun `getAdjacentPowerBlocks returns blocks in all 6 directions`() {
        val loc = TestHelper.createLocation(0.0, 64.0, 0.0)

        val east = Triple(1.0, 0.0, 0.0)
        val west = Triple(-1.0, 0.0, 0.0)
        val north = Triple(0.0, 0.0, -1.0)
        val south = Triple(0.0, 0.0, 1.0)
        val up = Triple(0.0, 1.0, 0.0)
        val down = Triple(0.0, -1.0, 0.0)

        val offsets =
            listOf(
                east,
                west,
                up,
                down,
                south,
                north,
            )

        for ((dx, dy, dz) in offsets) {
            val neighborLoc = TestHelper.createLocation(dx, 64.0 + dy, dz)
            TestHelper.addToRegistry(registry, SmallSolarPanel(neighborLoc), "small_solar_panel")
        }

        val adjacent = registry.getAdjacentPowerBlocks(loc)
        assertEquals(6, adjacent.size)
    }

    @Test
    fun `getAllPowerBlocksWithIds returns correct pairs`() {
        val loc1 = TestHelper.createLocation(0.0, 64.0, 0.0)
        val loc2 = TestHelper.createLocation(1.0, 64.0, 0.0)
        val block1 = SmallSolarPanel(loc1)
        val block2 = SmallBattery(loc2, BlockFace.NORTH)

        TestHelper.addToRegistry(registry, block1, "small_solar_panel")
        TestHelper.addToRegistry(registry, block2, "small_battery")

        val pairs = registry.getAllPowerBlocksWithIds()
        assertEquals(2, pairs.size)
        assertTrue(pairs.any { it.first === block1 && it.second == "small_solar_panel" })
        assertTrue(pairs.any { it.first === block2 && it.second == "small_battery" })
    }

    @Test
    fun `getAllPowerBlocks returns all registered blocks`() {
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation(0.0, 64.0, 0.0)), "sp")
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0)), "sp")
        assertEquals(2, registry.getAllPowerBlocks().size)
    }

    @Test
    fun `stopAll clears all blocks`() {
        TestHelper.addToRegistry(registry, SmallSolarPanel(TestHelper.createLocation()), "sp")
        registry.stopAll()
        assertEquals(0, registry.getAllPowerBlocks().size)
    }

    @Test
    fun `instance is set on creation`() {
        assertSame(registry, PowerBlockRegistry.instance)
    }

    @Test
    fun `getAdjacentPowerBlock returns null when no block in direction`() {
        val loc = TestHelper.createLocation(0.0, 64.0, 0.0)
        assertNull(registry.getAdjacentPowerBlock(loc, BlockFace.NORTH))
    }

    @Test
    fun `getAdjacentPowerBlocks returns empty when no neighbors`() {
        val loc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val adjacent = registry.getAdjacentPowerBlocks(loc)
        assertEquals(0, adjacent.size)
    }

    @Test
    fun `registering at same location overwrites`() {
        val loc = TestHelper.createLocation()
        val block1 = SmallSolarPanel(loc)
        val block2 = SmallSolarPanel(loc)
        TestHelper.addToRegistry(registry, block1, "small_solar_panel")
        TestHelper.addToRegistry(registry, block2, "small_solar_panel")

        assertSame(block2, registry.getPowerBlock(loc))
    }
}
