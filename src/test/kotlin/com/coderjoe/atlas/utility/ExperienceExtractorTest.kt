package com.coderjoe.atlas.utility

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callPowerUpdate
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.utility.block.ExperienceExtractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Hopper
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExperienceExtractorTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun mockHopperAt(
        x: Int,
        y: Int,
        z: Int,
        items: Array<ItemStack?>,
    ): Hopper {
        val block = mockk<Block>(relaxed = true)
        val hopper = mockk<Hopper>(relaxed = true)
        val inventory = mockk<Inventory>(relaxed = true)

        every { TestHelper.mockWorld.getBlockAt(x, y, z) } returns block
        every { block.state } returns hopper
        every { hopper.inventory } returns inventory
        every { inventory.size } returns items.size
        for (i in items.indices) {
            every { inventory.getItem(i) } returns items[i]
        }

        return hopper
    }

    private fun mockNonHopperAt(
        x: Int,
        y: Int,
        z: Int,
    ) {
        val block = mockk<Block>(relaxed = true)
        val blockState = mockk<org.bukkit.block.BlockState>(relaxed = true)
        every { TestHelper.mockWorld.getBlockAt(x, y, z) } returns block
        every { block.state } returns blockState
    }

    private fun setupAdjacentNonHoppers(
        centerX: Int = 0,
        centerY: Int = 64,
        centerZ: Int = 0,
        excludeFaces: Set<BlockFace> = emptySet(),
    ) {
        for (face in listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            if (face in excludeFaces) continue
            mockNonHopperAt(
                centerX + face.modX,
                centerY + face.modY,
                centerZ + face.modZ,
            )
        }
    }

    @Test
    fun `extractor has correct facing`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.EAST)
        assertEquals(BlockFace.EAST, extractor.facing)
    }

    @Test
    fun `extractor defaults to NORTH when SELF`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.SELF)
        assertEquals(BlockFace.NORTH, extractor.facing)
    }

    @Test
    fun `extractor base block ID is correct`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("atlas:experience_extractor", extractor.baseBlockId)
    }

    @Test
    fun `descriptor has correct properties`() {
        val desc = ExperienceExtractor.descriptor
        assertEquals("atlas:experience_extractor", desc.baseBlockId)
        assertEquals("Experience Extractor", desc.displayName)
        assertEquals(PlacementType.DIRECTIONAL, desc.placementType)
    }

    @Test
    fun `descriptor includes active variant`() {
        val desc = ExperienceExtractor.descriptor
        assertTrue(desc.additionalBlockIds.contains("atlas:experience_extractor_active"))
    }

    @Test
    fun `max storage is 12`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals(12, extractor.maxStorage)
    }

    @Test
    fun `canReceivePower is true`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        assertTrue(extractor.canAcceptPower())
    }

    @Test
    fun `visual state returns base when no power and no xp`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        extractor.currentPower = 0
        extractor.storedXp = 0.0
        assertEquals("atlas:experience_extractor", extractor.getVisualStateBlockId())
    }

    @Test
    fun `visual state returns active when has power`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        extractor.currentPower = 3
        assertEquals("atlas:experience_extractor_active", extractor.getVisualStateBlockId())
    }

    @Test
    fun `visual state returns active when has stored xp`() {
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        extractor.currentPower = 0
        extractor.storedXp = 2.0
        assertEquals("atlas:experience_extractor_active", extractor.getVisualStateBlockId())
    }

    @Test
    fun `base ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(PowerBlockFactory.isRegistered("atlas:experience_extractor"))
    }

    @Test
    fun `active ID is registered`() {
        TestHelper.initPowerFactory()
        assertTrue(PowerBlockFactory.isRegistered("atlas:experience_extractor_active"))
    }

    @Test
    fun `factory creates ExperienceExtractor from base ID`() {
        TestHelper.initPowerFactory()
        val block =
            PowerBlockFactory.createPowerBlock(
                "atlas:experience_extractor",
                TestHelper.createLocation(),
                BlockFace.SOUTH,
            )
        assertTrue(block is ExperienceExtractor)
        assertEquals(BlockFace.SOUTH, block!!.facing)
    }

    @Test
    fun `power update does not throw with no adjacent hoppers`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(), BlockFace.NORTH)
        setupAdjacentNonHoppers()

        assertDoesNotThrow {
            extractor.callPowerUpdate()
        }
    }

    @Test
    fun `pulls item from adjacent hopper and gains xp`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 3

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COAL
        every { mockStack.amount } returns 1

        val hopper = mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(0, extractor.currentPower)
        assertEquals(1.0, extractor.storedXp)
    }

    @Test
    fun `removes single item from hopper inventory`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 3

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COAL
        every { mockStack.amount } returns 1

        val hopper = mockHopperAt(0, 65, 0, arrayOf(mockStack))
        val inventory = hopper.inventory

        extractor.callPowerUpdate()

        verify { inventory.setItem(0, null) }
    }

    @Test
    fun `decrements hopper item stack when amount greater than 1`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 3

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COAL
        every { mockStack.amount } returns 5

        mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        verify { mockStack.amount = 4 }
    }

    @Test
    fun `does not pull items without power`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 0
        setupAdjacentNonHoppers(excludeFaces = setOf(BlockFace.UP))

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COAL
        every { mockStack.amount } returns 1

        mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(0.0, extractor.storedXp)
    }

    @Test
    fun `unlisted items give default xp`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 6
        setupAdjacentNonHoppers(excludeFaces = setOf(BlockFace.UP))

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COBBLESTONE
        every { mockStack.amount } returns 1

        mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(3, extractor.currentPower)
        assertEquals(ExperienceExtractor.DEFAULT_XP, extractor.storedXp)
    }

    @Test
    fun `pushes experience fluid to adjacent fluid block in facing direction`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val extractorLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val extractor = ExperienceExtractor(extractorLoc, BlockFace.NORTH)
        extractor.storedXp = 3.0
        TestHelper.addToRegistry(powerRegistry, extractor, ExperienceExtractor.BLOCK_ID)
        setupAdjacentNonHoppers()

        val pipeLoc = TestHelper.createLocation(0.0, 64.0, -1.0)
        val pipe = FluidPipe(pipeLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(fluidRegistry, pipe, FluidPipe.BLOCK_ID)

        extractor.callPowerUpdate()

        assertEquals(2.0, extractor.storedXp)
        assertEquals(FluidType.EXPERIENCE, pipe.storedFluid)
    }

    @Test
    fun `does not push fluid when no adjacent fluid block`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        FluidBlockRegistry(TestHelper.mockPlugin)

        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.storedXp = 3.0
        setupAdjacentNonHoppers()

        extractor.callPowerUpdate()

        assertEquals(3.0, extractor.storedXp)
    }

    @Test
    fun `does not consume items when xp buffer is full`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 6
        extractor.storedXp = ExperienceExtractor.MAX_XP_BUFFER
        setupAdjacentNonHoppers(excludeFaces = setOf(BlockFace.UP))

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.COAL
        every { mockStack.amount } returns 1

        mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(6, extractor.currentPower)
    }

    @Test
    fun `xp values map contains expected categories`() {
        val xpValues = ExperienceExtractor.XP_VALUES

        // Ore blocks
        assertEquals(1.0, xpValues[Material.IRON_ORE])
        assertEquals(1.0, xpValues[Material.DEEPSLATE_IRON_ORE])
        assertEquals(1.0, xpValues[Material.COAL_ORE])
        assertEquals(1.0, xpValues[Material.COPPER_ORE])
        assertEquals(1.0, xpValues[Material.GOLD_ORE])
        // Raw ores & minerals
        assertEquals(1.0, xpValues[Material.RAW_IRON])
        assertEquals(1.0, xpValues[Material.RAW_GOLD])
        assertEquals(1.0, xpValues[Material.COAL])
        // Food
        assertEquals(2.0, xpValues[Material.COOKED_BEEF])
        assertEquals(2.0, xpValues[Material.COOKED_CHICKEN])
        // Monster drops
        assertEquals(3.0, xpValues[Material.BONE])
        assertEquals(3.0, xpValues[Material.GUNPOWDER])
        // Valuable ores & drops
        assertEquals(5.0, xpValues[Material.DIAMOND_ORE])
        assertEquals(5.0, xpValues[Material.DEEPSLATE_DIAMOND_ORE])
        assertEquals(5.0, xpValues[Material.BLAZE_ROD])
        assertEquals(5.0, xpValues[Material.DIAMOND])
        // Very valuable
        assertEquals(8.0, xpValues[Material.NETHER_STAR])
    }

    @Test
    fun `pulls power from adjacent blocks`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        val extractorLoc = TestHelper.createLocation(0.0, 64.0, 0.0)
        val extractor = ExperienceExtractor(extractorLoc, BlockFace.NORTH)
        TestHelper.addToRegistry(registry, extractor, ExperienceExtractor.BLOCK_ID)
        setupAdjacentNonHoppers()

        val batteryLoc = TestHelper.createLocation(1.0, 64.0, 0.0)
        val battery = com.coderjoe.atlas.power.block.SmallBattery(batteryLoc, BlockFace.WEST)
        battery.currentPower = 5
        TestHelper.addToRegistry(registry, battery, "atlas:small_battery")

        extractor.callPowerUpdate()

        assertTrue(extractor.currentPower > 0)
        assertTrue(battery.currentPower < 5)
    }

    @Test
    fun `pulls ore blocks from hopper`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 3

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.IRON_ORE
        every { mockStack.amount } returns 1

        val hopper = mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(1.0, extractor.storedXp)
    }

    @Test
    fun `higher value items give more xp`() {
        PowerBlockRegistry(TestHelper.mockPlugin)
        val extractor = ExperienceExtractor(TestHelper.createLocation(0.0, 64.0, 0.0), BlockFace.NORTH)
        extractor.currentPower = 3
        setupAdjacentNonHoppers(excludeFaces = setOf(BlockFace.UP))

        val mockStack = mockk<ItemStack>(relaxed = true)
        every { mockStack.type } returns Material.NETHER_STAR
        every { mockStack.amount } returns 1

        mockHopperAt(0, 65, 0, arrayOf(mockStack))

        extractor.callPowerUpdate()

        assertEquals(8.0, extractor.storedXp)
    }
}
