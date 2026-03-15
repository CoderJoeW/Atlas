package com.coderjoe.atlas.power

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.block.SmallDrill
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame

class SmallDrillMiningTest {

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

    private fun mockBlockAt(x: Int, y: Int, z: Int, material: Material): Block {
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns material
        every { block.getDrops() } returns emptyList()
        every { TestHelper.mockWorld.getBlockAt(x, y, z) } returns block
        return block
    }

    @Test
    fun `drill disabled does not mine or pull power`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.enabled = false
        drill.currentPower = 10

        drill.callPowerUpdate()
        assertEquals(10, drill.currentPower) // power unchanged
    }

    @Test
    fun `drill with insufficient power pulls but does not mine`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 5

        // Set up a stone block below — should not be mined since power < 10
        mockBlockAt(0, 63, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(5, drill.currentPower)
    }

    @Test
    fun `drill with full power mines first non-air block below`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 10

        // Air at y=63, stone at y=62
        mockBlockAt(0, 63, 0, Material.AIR)
        val stoneBlock = mockBlockAt(0, 62, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill skips air variants when scanning downward`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 10

        mockBlockAt(0, 63, 0, Material.AIR)
        mockBlockAt(0, 62, 0, Material.CAVE_AIR)
        mockBlockAt(0, 61, 0, Material.VOID_AIR)
        val stoneBlock = mockBlockAt(0, 60, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill stops at bedrock without mining`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 10

        mockBlockAt(0, 63, 0, Material.BEDROCK)

        drill.callPowerUpdate()
        assertEquals(10, drill.currentPower) // no power consumed
    }

    @Test
    fun `drill mines horizontally facing NORTH`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        drill.currentPower = 10

        // NORTH = z-1
        mockBlockAt(0, 64, -1, Material.AIR)
        val stoneBlock = mockBlockAt(0, 64, -2, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill respects 64-block horizontal range limit`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)
        drill.currentPower = 10

        // All blocks in range are AIR
        for (i in 1..64) {
            mockBlockAt(i, 64, 0, Material.AIR)
        }

        drill.callPowerUpdate()
        // No block to mine, power unchanged
        assertEquals(10, drill.currentPower)
    }

    @Test
    fun `drill pulls power from adjacent neighbors`() {
        val drillLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val drill = SmallDrill(drillLoc, BlockFace.DOWN)
        drill.currentPower = 0

        // Place powered solar panels around the drill
        val source1 = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source1.currentPower = 1
        val source2 = SmallSolarPanel(TestHelper.createLocation(-1.0, 64.0, 0.0))
        source2.currentPower = 1

        TestHelper.addToRegistry(registry, drill, "small_drill_down")
        TestHelper.addToRegistry(registry, source1, "small_solar_panel")
        TestHelper.addToRegistry(registry, source2, "small_solar_panel")

        // Mock blocks below so drill doesn't crash during mining scan
        for (y in 63 downTo -64) {
            mockBlockAt(0, y, 0, Material.AIR)
        }

        drill.callPowerUpdate()
        assertEquals(2, drill.currentPower) // pulled 1 from each
    }

    @Test
    fun `drill mines horizontally facing SOUTH`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.SOUTH)
        drill.currentPower = 10

        // SOUTH = z+1
        mockBlockAt(0, 64, 1, Material.AIR)
        val stoneBlock = mockBlockAt(0, 64, 2, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill mines horizontally facing EAST`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.EAST)
        drill.currentPower = 10

        // EAST = x+1
        val stoneBlock = mockBlockAt(1, 64, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill mines horizontally facing WEST`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.WEST)
        drill.currentPower = 10

        // WEST = x-1
        val stoneBlock = mockBlockAt(-1, 64, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `drill stops at bedrock in horizontal mining`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        drill.currentPower = 10

        mockBlockAt(0, 64, -1, Material.BEDROCK)

        drill.callPowerUpdate()
        assertEquals(10, drill.currentPower) // no power consumed
    }

    @Test
    fun `drill all-air column to minHeight does not mine`() {
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        drill.currentPower = 10

        // All air from y=63 down to minHeight=-64
        for (y in 63 downTo -64) {
            mockBlockAt(0, y, 0, Material.AIR)
        }

        drill.callPowerUpdate()
        assertEquals(10, drill.currentPower)
    }

    @Test
    fun `drill stops pulling power when full mid-loop`() {
        val drillLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val drill = SmallDrill(drillLoc, BlockFace.DOWN)
        drill.currentPower = 9 // needs 1 more to be full

        val source1 = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        source1.currentPower = 1
        val source2 = SmallSolarPanel(TestHelper.createLocation(-1.0, 64.0, 0.0))
        source2.currentPower = 1

        TestHelper.addToRegistry(registry, drill, "small_drill_down")
        TestHelper.addToRegistry(registry, source1, "small_solar_panel")
        TestHelper.addToRegistry(registry, source2, "small_solar_panel")

        // Mock blocks below - stone at 63 so it mines
        val stoneBlock = mockBlockAt(0, 63, 0, Material.STONE)

        drill.callPowerUpdate()
        // Drill pulls 1 power (to reach 10), then mines (uses 10, back to 0)
        assertEquals(0, drill.currentPower)
        // One source should still have its power
        val totalRemaining = source1.currentPower + source2.currentPower
        assertEquals(1, totalRemaining)
    }

    @Test
    fun `drill facing UP uses DOWN branch logic - known bug`() {
        // Document known bug: UP falls into horizontal branch where modX=0, modZ=0
        // This means it checks the same position (drill's own x,y,z) 64 times
        val drill = SmallDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.UP)
        drill.currentPower = 10

        // Since modX and modZ are both 0 for UP, getBlockAt will get (0, 64, 0) repeatedly
        val selfBlock = mockBlockAt(0, 64, 0, Material.AIR)

        drill.callPowerUpdate()
        // No mining happens, power stays at 10
        assertEquals(10, drill.currentPower)
    }
}
