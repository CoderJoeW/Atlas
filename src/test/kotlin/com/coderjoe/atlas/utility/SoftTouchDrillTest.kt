package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.utility.block.SoftTouchDrill
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SoftTouchDrillTest {
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

    private fun mockBlockAt(
        x: Int,
        y: Int,
        z: Int,
        material: Material,
    ): Block {
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns material
        every { block.getDrops(any<ItemStack>()) } returns emptyList()
        every { TestHelper.mockWorld.getBlockAt(x, y, z) } returns block
        return block
    }

    private fun createDrill(
        x: Double,
        y: Double,
        z: Double,
        facing: BlockFace,
    ): SoftTouchDrill {
        val drill = spyk(SoftTouchDrill(TestHelper.createLocation(x, y, z), facing))
        every { drill.getBlockDrops(any()) } returns emptyList()
        return drill
    }

    @Test
    fun `soft touch drill has max storage of 40`() {
        val drill = SoftTouchDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        assertEquals(40, drill.maxStorage)
    }

    @Test
    fun `soft touch drill requires 20 power to mine`() {
        val drill = createDrill(0.0, 64.0, 0.0, BlockFace.DOWN)
        drill.currentPower = 19

        mockBlockAt(0, 63, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(19, drill.currentPower)
    }

    @Test
    fun `soft touch drill mines with 20 power`() {
        val drill = createDrill(0.0, 64.0, 0.0, BlockFace.DOWN)
        drill.currentPower = 20

        val stoneBlock = mockBlockAt(0, 63, 0, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `soft touch drill calls getBlockDrops when mining`() {
        val drill = createDrill(0.0, 64.0, 0.0, BlockFace.DOWN)
        drill.currentPower = 20

        val stoneBlock = mockBlockAt(0, 63, 0, Material.STONE)

        drill.callPowerUpdate()
        verify { drill.getBlockDrops(stoneBlock) }
    }

    @Test
    fun `soft touch drill has correct block id`() {
        val drill = SoftTouchDrill(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.DOWN)
        assertEquals("atlas:soft_touch_drill", drill.baseBlockId)
    }

    @Test
    fun `soft touch drill mines horizontally`() {
        val drill = createDrill(0.0, 64.0, 0.0, BlockFace.NORTH)
        drill.currentPower = 20

        val stoneBlock = mockBlockAt(0, 64, -1, Material.STONE)

        drill.callPowerUpdate()
        assertEquals(0, drill.currentPower)
        verify { stoneBlock.setType(Material.AIR, false) }
    }
}
