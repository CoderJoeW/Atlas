package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.block.SmallSolarPanel
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FluidBlockLogicTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    // --- FluidBlock base class ---

    @Test
    fun `hasFluid returns false when NONE`() {
        val pump = FluidPump(TestHelper.createLocation())
        assertFalse(pump.hasFluid())
    }

    @Test
    fun `hasFluid returns true when WATER`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertTrue(pump.hasFluid())
    }

    @Test
    fun `hasFluid returns true when LAVA`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.LAVA)
        assertTrue(pump.hasFluid())
    }

    @Test
    fun `storeFluid on empty block returns true`() {
        val pump = FluidPump(TestHelper.createLocation())
        assertTrue(pump.storeFluid(FluidType.WATER))
        assertEquals(FluidType.WATER, pump.storedFluid)
    }

    @Test
    fun `storeFluid on block already holding fluid returns false`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertFalse(pump.storeFluid(FluidType.LAVA))
        assertEquals(FluidType.WATER, pump.storedFluid) // unchanged
    }

    @Test
    fun `removeFluid returns stored fluid and resets to NONE`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        val removed = pump.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `removeFluid on empty block returns NONE`() {
        val pump = FluidPump(TestHelper.createLocation())
        assertEquals(FluidType.NONE, pump.removeFluid())
    }

    // --- FluidPump specifics ---

    @Test
    fun `pump visual state NONE`() {
        val pump = FluidPump(TestHelper.createLocation())
        assertEquals("atlas:fluid_pump", pump.getVisualStateBlockId())
    }

    @Test
    fun `pump visual state WATER`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertEquals("atlas:fluid_pump_active", pump.getVisualStateBlockId())
    }

    @Test
    fun `pump visual state LAVA`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.LAVA)
        assertEquals(
            "atlas:fluid_pump_active_lava",
            pump.getVisualStateBlockId(),
        )
    }

    @Test
    fun `pump canRemoveFluidFrom returns false when no cauldron face`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertFalse(pump.canRemoveFluidFrom(BlockFace.NORTH))
    }

    @Test
    fun `pump status starts as NO_SOURCE`() {
        val pump = FluidPump(TestHelper.createLocation())
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate when holding fluid sets IDLE`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)

        pump.callFluidUpdate()
        assertEquals(FluidPump.PumpStatus.IDLE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with no adjacent cauldron sets NO_SOURCE`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        for (face in listOf(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        pump.callFluidUpdate()
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with cauldron but no power sets NO_POWER`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        pump.callFluidUpdate()
        assertEquals(FluidPump.PumpStatus.NO_POWER, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with water cauldron and power extracts water`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(0, solar.currentPower) // power consumed
    }

    @Test
    fun `pump fluidUpdate with lava cauldron and power stores LAVA`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.LAVA_CAULDRON
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
    }

    @Test
    fun `pump canRemoveFluidFrom returns true when direction matches`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        val field =
            FluidPump::class.java.getDeclaredField("cauldronFace")
        field.isAccessible = true
        field.set(pump, BlockFace.NORTH)

        assertTrue(pump.canRemoveFluidFrom(BlockFace.SOUTH))
    }

    @Test
    fun `pump canRemoveFluidFrom returns false for wrong direction`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        val field =
            FluidPump::class.java.getDeclaredField("cauldronFace")
        field.isAccessible = true
        field.set(pump, BlockFace.NORTH)

        assertFalse(pump.canRemoveFluidFrom(BlockFace.EAST))
    }

    @Test
    fun `pump canRemoveFluidFrom returns false when no fluid`() {
        val pump = FluidPump(TestHelper.createLocation())
        val field =
            FluidPump::class.java.getDeclaredField("cauldronFace")
        field.isAccessible = true
        field.set(pump, BlockFace.NORTH)

        assertFalse(pump.canRemoveFluidFrom(BlockFace.SOUTH))
    }

    @Test
    fun `pump isPowered reflects adjacent power blocks`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertTrue(pump.isPowered)
    }

    @Test
    fun `pump isPowered false when no adjacent power`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        pump.callFluidUpdate()
        assertFalse(pump.isPowered)
    }

    @Test
    fun `pump water cauldron level 1 empties to CAULDRON`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 1
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)
        io.mockk.verify {
            cauldronBlock.setType(Material.CAULDRON, false)
        }
    }

    @Test
    fun `pump water cauldron level 3 decrements to level 2`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        io.mockk.verify { levelled.level = 2 }
        io.mockk.verify { cauldronBlock.blockData = levelled }
    }

    @Test
    fun `pump lava cauldron fully consumed`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.LAVA_CAULDRON
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns cauldronBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        io.mockk.verify {
            cauldronBlock.setType(Material.CAULDRON, false)
        }
    }

    @Test
    fun `pump extracts water from source block`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val waterBlock = mockk<Block>(relaxed = true)
        val levelled = mockk<Levelled>(relaxed = true)
        every { waterBlock.type } returns Material.WATER
        every { waterBlock.blockData } returns levelled
        every { levelled.level } returns 0
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns waterBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        io.mockk.verify { waterBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `pump extracts lava from source block`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val lavaBlock = mockk<Block>(relaxed = true)
        val levelled = mockk<Levelled>(relaxed = true)
        every { lavaBlock.type } returns Material.LAVA
        every { lavaBlock.blockData } returns levelled
        every { levelled.level } returns 0
        every {
            TestHelper.mockWorld.getBlockAt(0, 64, -1)
        } returns lavaBlock

        for (face in listOf(
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every {
                TestHelper.mockWorld.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        val solar = SmallSolarPanel(TestHelper.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        TestHelper.addToRegistry(
            powerRegistry,
            solar,
            "atlas:small_solar_panel",
        )

        pump.callFluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        io.mockk.verify { lavaBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `pump ignores flowing water (non-source block)`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))

        val flowingBlock = mockk<Block>(relaxed = true)
        val levelled = mockk<Levelled>(relaxed = true)
        every { flowingBlock.type } returns Material.WATER
        every { flowingBlock.blockData } returns levelled
        every { levelled.level } returns 3

        for (face in listOf(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )) {
            val offset = face.direction
            if (face == BlockFace.NORTH) {
                every {
                    TestHelper.mockWorld.getBlockAt(
                        offset.blockX, 64 + offset.blockY, offset.blockZ,
                    )
                } returns flowingBlock
            } else {
                val block = mockk<Block>(relaxed = true)
                every { block.type } returns Material.AIR
                every {
                    TestHelper.mockWorld.getBlockAt(
                        offset.blockX, 64 + offset.blockY, offset.blockZ,
                    )
                } returns block
            }
        }

        pump.callFluidUpdate()
        assertEquals(FluidType.NONE, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    // --- FluidPipe specifics ---

    @Test
    fun `pipe visual state returns BLOCK_ID`() {
        val pipe = FluidPipe(TestHelper.createLocation(), BlockFace.NORTH)
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe visual state returns BLOCK_ID regardless of fluid`() {
        val pipe = FluidPipe(TestHelper.createLocation(), BlockFace.EAST)
        pipe.storeFluid(FluidType.WATER)
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe fluidUpdate does nothing when already holding fluid`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)
        val pipe = FluidPipe(TestHelper.createLocation(), BlockFace.NORTH)
        pipe.storeFluid(FluidType.WATER)

        pipe.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe.storedFluid) // unchanged
    }

    @Test
    fun `pipe pulls from FluidPump behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.SOUTH,
            )
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        pump.storeFluid(FluidType.WATER)

        val cauldronFaceField =
            FluidPump::class.java.getDeclaredField("cauldronFace")
        cauldronFaceField.isAccessible = true
        cauldronFaceField.set(pump, BlockFace.NORTH)

        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )
        TestHelper.addToRegistry(
            fluidRegistry,
            pump,
            "atlas:fluid_pump",
        )

        pipe.callFluidUpdate()
        assertEquals(FluidType.WATER, pipe.storedFluid)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `pipe pulls from FluidPipe behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe1 =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.SOUTH,
            )
        val pipe2 =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, -1.0),
                BlockFace.SOUTH,
            )
        pipe2.storeFluid(FluidType.LAVA)

        TestHelper.addToRegistry(
            fluidRegistry,
            pipe1,
            "atlas:fluid_pipe",
        )
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe2,
            "atlas:fluid_pipe",
        )

        pipe1.callFluidUpdate()
        assertEquals(FluidType.LAVA, pipe1.storedFluid)
        assertEquals(FluidType.NONE, pipe2.storedFluid)
    }

    @Test
    fun `pipe does nothing when source has no fluid`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.SOUTH,
            )
        val sourcePipe =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, -1.0),
                BlockFace.SOUTH,
            )

        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )
        TestHelper.addToRegistry(
            fluidRegistry,
            sourcePipe,
            "atlas:fluid_pipe",
        )

        pipe.callFluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }

    @Test
    fun `pipe does nothing when no fluid block behind it`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe =
            FluidPipe(
                TestHelper.createLocation(0.0, 64.0, 0.0),
                BlockFace.SOUTH,
            )
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        pipe.callFluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }
}
