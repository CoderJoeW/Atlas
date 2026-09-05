package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.TestHelper.callFluidUpdate
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import com.coderjoe.atlas.power.PowerBlockRegistry
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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.callFluidUpdate()
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(0, pump.storedPower, "the extraction spent the buffered unit")
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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.callFluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
    }

    @Test
    fun `pump gives fluid out regardless of where its source was`() {
        val pump = FluidPump(TestHelper.createLocation())
        pump.storeFluid(FluidType.WATER)
        val field = FluidPump::class.java.getDeclaredField("cauldronFace")
        field.isAccessible = true
        field.set(pump, BlockFace.NORTH)

        // the source side used to dictate a single output face; it no longer does
        assertTrue(pump.canProvideFluid(BlockFace.SOUTH))
        assertTrue(pump.canProvideFluid(BlockFace.EAST))
        assertTrue(pump.canProvideFluid(BlockFace.NORTH))
    }

    @Test
    fun `pump reports powered once a run has pushed power into it`() {
        val powerRegistry = PowerBlockRegistry(TestHelper.mockPlugin)
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, 0.0))
        pump.storeFluid(FluidType.WATER)

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.callFluidUpdate()
        assertTrue(pump.isPowered)
    }

    @Test
    fun `pump reports unpowered while its buffer is empty`() {
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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

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

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

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
        val pipe = FluidPipe(TestHelper.createLocation())
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe visual state returns BLOCK_ID regardless of what the run carries`() {
        val pipe = FluidPipe(TestHelper.createLocation())
        pipe.carrying = FluidType.WATER
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe never stores fluid of its own`() {
        FluidBlockRegistry(TestHelper.mockPlugin)
        val pipe = FluidPipe(TestHelper.createLocation())

        // storeFluid on a pipe is a request to hand the unit to the run, and an isolated run has
        // nowhere to put it, so it is refused rather than swallowed
        assertFalse(pipe.storeFluid(FluidType.WATER))
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }

    @Test
    fun `pipe offers what the pump on its run is holding`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0))
        val pump = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        pump.storeFluid(FluidType.WATER)

        val cauldronFaceField = FluidPump::class.java.getDeclaredField("cauldronFace")
        cauldronFaceField.isAccessible = true
        cauldronFaceField.set(pump, BlockFace.NORTH)

        TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")
        TestHelper.addToRegistry(fluidRegistry, pump, "atlas:fluid_pump")

        assertTrue(pipe.hasFluid(), "the run has a loaded pump on it")
        assertEquals(FluidType.WATER, pipe.removeFluid(), "drawing from the pipe draws from the pump")
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `two joined pipes are one run and neither holds anything`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe1 = FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0))
        val pipe2 = FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))

        TestHelper.addToRegistry(fluidRegistry, pipe1, "atlas:fluid_pipe")
        TestHelper.addToRegistry(fluidRegistry, pipe2, "atlas:fluid_pipe")

        pipe1.callFluidUpdate()

        assertEquals(FluidType.NONE, pipe1.storedFluid)
        assertEquals(FluidType.NONE, pipe2.storedFluid)
        assertTrue(BlockFace.NORTH in pipe1.connections(), "the pipes should join each other")
    }

    @Test
    fun `a lava run and a water run that meet stay separate networks`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        // lava pump - pipe - pipe | pipe - pipe - water pump, laid out along the Z axis
        val lavaPump = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        lavaPump.storeFluid(FluidType.LAVA)
        val waterPump = FluidPump(TestHelper.createLocation(0.0, 64.0, 4.0))
        waterPump.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(TestHelper.createLocation(0.0, 64.0, it.toDouble())) }
        TestHelper.addToRegistry(fluidRegistry, lavaPump, "atlas:fluid_pump")
        TestHelper.addToRegistry(fluidRegistry, waterPump, "atlas:fluid_pump")
        for (pipe in pipes) TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        val lavaRun = FluidNetworks.networkFor(pipes[0]).pipes
        val waterRun = FluidNetworks.networkFor(pipes[3]).pipes

        assertEquals(2, lavaRun.size, "the two pipes nearest the lava pump are the lava run")
        assertEquals(2, waterRun.size, "the two pipes nearest the water pump are the water run")
        assertTrue(lavaRun.none { it in waterRun }, "no pipe belongs to both runs")

        assertEquals(FluidType.LAVA, FluidNetworks.networkFor(pipes[0]).availableFluid())
        assertEquals(FluidType.WATER, FluidNetworks.networkFor(pipes[3]).availableFluid())
    }

    @Test
    fun `pipe arms stop where a lava run meets a water run`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val lavaPump = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        lavaPump.storeFluid(FluidType.LAVA)
        val waterPump = FluidPump(TestHelper.createLocation(0.0, 64.0, 4.0))
        waterPump.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(TestHelper.createLocation(0.0, 64.0, it.toDouble())) }
        TestHelper.addToRegistry(fluidRegistry, lavaPump, "atlas:fluid_pump")
        TestHelper.addToRegistry(fluidRegistry, waterPump, "atlas:fluid_pump")
        for (pipe in pipes) TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        // SOUTH is +Z, so this is the arm pointing across the seam at the water side
        assertFalse(BlockFace.SOUTH in pipes[1].connections(), "the lava pipe should not reach across")
        assertFalse(BlockFace.NORTH in pipes[2].connections(), "nor the water pipe back")
        assertTrue(BlockFace.NORTH in pipes[1].connections(), "but it still joins its own run")
    }

    @Test
    fun `an unfed run is still one network`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipes = (0..3).map { FluidPipe(TestHelper.createLocation(0.0, 64.0, it.toDouble())) }
        for (pipe in pipes) TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        // nothing labels these pipes, so they must not fragment into one network each
        assertEquals(4, FluidNetworks.networkFor(pipes[0]).pipes.size)
    }

    @Test
    fun `two runs on the same fluid still join`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val left = FluidPump(TestHelper.createLocation(0.0, 64.0, -1.0))
        left.storeFluid(FluidType.WATER)
        val right = FluidPump(TestHelper.createLocation(0.0, 64.0, 4.0))
        right.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(TestHelper.createLocation(0.0, 64.0, it.toDouble())) }
        TestHelper.addToRegistry(fluidRegistry, left, "atlas:fluid_pump")
        TestHelper.addToRegistry(fluidRegistry, right, "atlas:fluid_pump")
        for (pipe in pipes) TestHelper.addToRegistry(fluidRegistry, pipe, "atlas:fluid_pipe")

        assertEquals(4, FluidNetworks.networkFor(pipes[0]).pipes.size, "same fluid, so one run")
    }

    @Test
    fun `pipe does nothing when source has no fluid`() {
        val fluidRegistry = FluidBlockRegistry(TestHelper.mockPlugin)

        val pipe =
            FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0))
        val sourcePipe =
            FluidPipe(TestHelper.createLocation(0.0, 64.0, -1.0))

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
            FluidPipe(TestHelper.createLocation(0.0, 64.0, 0.0))
        TestHelper.addToRegistry(
            fluidRegistry,
            pipe,
            "atlas:fluid_pipe",
        )

        pipe.callFluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }
}
