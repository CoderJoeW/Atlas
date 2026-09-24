package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.testing.Blocks.placedIn
import com.coderjoe.atlas.testing.MockServer
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

    // --- FluidBlock base class ---

    @Test
    fun `hasFluid returns false when NONE`() {
        val pump = FluidPump(MockServer.createLocation())
        assertFalse(pump.hasFluid())
    }

    @Test
    fun `hasFluid returns true when WATER`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertTrue(pump.hasFluid())
    }

    @Test
    fun `hasFluid returns true when LAVA`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.LAVA)
        assertTrue(pump.hasFluid())
    }

    @Test
    fun `storeFluid on empty block returns true`() {
        val pump = FluidPump(MockServer.createLocation())
        assertTrue(pump.storeFluid(FluidType.WATER))
        assertEquals(FluidType.WATER, pump.storedFluid)
    }

    @Test
    fun `storeFluid on block already holding fluid returns false`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.WATER)
        assertFalse(pump.storeFluid(FluidType.LAVA))
        assertEquals(FluidType.WATER, pump.storedFluid) // unchanged
    }

    @Test
    fun `removeFluid returns stored fluid and resets to NONE`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.WATER)
        val removed = pump.removeFluid()
        assertEquals(FluidType.WATER, removed)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `removeFluid on empty block returns NONE`() {
        val pump = FluidPump(MockServer.createLocation())
        assertEquals(FluidType.NONE, pump.removeFluid())
    }

    // --- FluidPump specifics ---

    @Test
    fun `pump status starts as NO_SOURCE`() {
        val pump = FluidPump(MockServer.createLocation())
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate when holding fluid sets IDLE`() {
        val pump = FluidPump(MockServer.createLocation()).placedIn(registry)
        pump.storeFluid(FluidType.WATER)

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.IDLE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with no adjacent cauldron sets NO_SOURCE`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with cauldron but no power sets NO_POWER`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.NO_POWER, pump.pumpStatus)
    }

    @Test
    fun `pump fluidUpdate with water cauldron and power extracts water`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(0, pump.storedPower, "the extraction spent the buffered unit")
    }

    @Test
    fun `pump fluidUpdate with lava cauldron and power stores LAVA`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.LAVA_CAULDRON
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
    }

    @Test
    fun `pump gives fluid out regardless of where its source was`() {
        val pump = FluidPump(MockServer.createLocation())
        pump.storeFluid(FluidType.WATER)
        pump.cauldronFace = BlockFace.NORTH

        // the source side used to dictate a single output face; it no longer does
        assertTrue(pump.canProvideFluid(BlockFace.SOUTH))
        assertTrue(pump.canProvideFluid(BlockFace.EAST))
        assertTrue(pump.canProvideFluid(BlockFace.NORTH))
    }

    @Test
    fun `pump reports powered once a run has pushed power into it`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)
        pump.storeFluid(FluidType.WATER)

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertTrue(pump.isPowered)
    }

    @Test
    fun `pump reports unpowered while its buffer is empty`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)
        pump.storeFluid(FluidType.WATER)

        pump.fluidUpdate()
        assertFalse(pump.isPowered)
    }

    @Test
    fun `pump water cauldron level 1 empties to CAULDRON`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 1
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)
        io.mockk.verify {
            cauldronBlock.setType(Material.CAULDRON, false)
        }
    }

    @Test
    fun `pump water cauldron level 3 decrements to level 2`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        io.mockk.verify { levelled.level = 2 }
        io.mockk.verify { cauldronBlock.blockData = levelled }
    }

    @Test
    fun `pump lava cauldron fully consumed`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.LAVA_CAULDRON
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        io.mockk.verify {
            cauldronBlock.setType(Material.CAULDRON, false)
        }
    }

    @Test
    fun `pump extracts water from source block`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val waterBlock = mockk<Block>(relaxed = true)
        val levelled = mockk<Levelled>(relaxed = true)
        every { waterBlock.type } returns Material.WATER
        every { waterBlock.blockData } returns levelled
        every { levelled.level } returns 0
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        io.mockk.verify { waterBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `pump extracts lava from source block`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

        val lavaBlock = mockk<Block>(relaxed = true)
        val levelled = mockk<Levelled>(relaxed = true)
        every { lavaBlock.type } returns Material.LAVA
        every { lavaBlock.blockData } returns levelled
        every { levelled.level } returns 0
        every {
            MockServer.world.getBlockAt(0, 64, -1)
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
                MockServer.world.getBlockAt(
                    offset.blockX, 64 + offset.blockY, offset.blockZ,
                )
            } returns block
        }

        // power is pushed to the pump by the run, not taken by it, so fill its buffer
        pump.acceptPower(BlockFace.EAST, FluidPump.POWER_PER_EXTRACT)

        pump.fluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        io.mockk.verify { lavaBlock.setType(Material.AIR, false) }
    }

    @Test
    fun `pump ignores flowing water (non-source block)`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0)).placedIn(registry)

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
                    MockServer.world.getBlockAt(
                        offset.blockX, 64 + offset.blockY, offset.blockZ,
                    )
                } returns flowingBlock
            } else {
                val block = mockk<Block>(relaxed = true)
                every { block.type } returns Material.AIR
                every {
                    MockServer.world.getBlockAt(
                        offset.blockX, 64 + offset.blockY, offset.blockZ,
                    )
                } returns block
            }
        }

        pump.fluidUpdate()
        assertEquals(FluidType.NONE, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump.pumpStatus)
    }

    // --- FluidPipe specifics ---

    @Test
    fun `pipe visual state returns BLOCK_ID`() {
        val pipe = FluidPipe(MockServer.createLocation())
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe visual state returns BLOCK_ID regardless of what the run carries`() {
        val pipe = FluidPipe(MockServer.createLocation())
        pipe.carrying = FluidType.WATER
        assertEquals("atlas:fluid_pipe", pipe.getVisualStateBlockId())
    }

    @Test
    fun `pipe never stores fluid of its own`() {
        val pipe = FluidPipe(MockServer.createLocation()).placedIn(registry)

        // storeFluid on a pipe is a request to hand the unit to the run, and an isolated run has
        // nowhere to put it, so it is refused rather than swallowed
        assertFalse(pipe.storeFluid(FluidType.WATER))
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }

    @Test
    fun `pipe offers what the pump on its run is holding`() {
        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 0.0))
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        pump.storeFluid(FluidType.WATER)

        pump.cauldronFace = BlockFace.NORTH

        registry.track(pipe, "atlas:fluid_pipe")
        registry.track(pump, "atlas:fluid_pump")

        assertTrue(pipe.hasFluid(), "the run has a loaded pump on it")
        assertEquals(FluidType.WATER, pipe.removeFluid(), "drawing from the pipe draws from the pump")
        assertEquals(FluidType.NONE, pump.storedFluid)
    }

    @Test
    fun `two joined pipes are one run and neither holds anything`() {
        val pipe1 = FluidPipe(MockServer.createLocation(0.0, 64.0, 0.0))
        val pipe2 = FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))

        registry.track(pipe1, "atlas:fluid_pipe")
        registry.track(pipe2, "atlas:fluid_pipe")

        pipe1.fluidUpdate()

        assertEquals(FluidType.NONE, pipe1.storedFluid)
        assertEquals(FluidType.NONE, pipe2.storedFluid)
        assertTrue(BlockFace.NORTH in pipe1.connections(), "the pipes should join each other")
    }

    @Test
    fun `a pipe grows an arm toward a lava generator, a consumer from another registry`() {
        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(pipe, "atlas:fluid_pipe")

        val generator = LavaGenerator(MockServer.createLocation(0.0, 64.0, -1.0))
        registry.track(generator, "atlas:lava_generator")

        // the generator sits at -Z from the pipe, so NORTH is the arm pointing at it
        assertTrue(BlockFace.NORTH in pipe.connections(), "the pipe should join a consumer from another registry")
    }

    @Test
    fun `a lava run and a water run that meet stay separate networks`() {
        // lava pump - pipe - pipe | pipe - pipe - water pump, laid out along the Z axis
        val lavaPump = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        lavaPump.storeFluid(FluidType.LAVA)
        val waterPump = FluidPump(MockServer.createLocation(0.0, 64.0, 4.0))
        waterPump.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(MockServer.createLocation(0.0, 64.0, it.toDouble())) }
        registry.track(lavaPump, "atlas:fluid_pump")
        registry.track(waterPump, "atlas:fluid_pump")
        for (pipe in pipes) registry.track(pipe, "atlas:fluid_pipe")

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
        val lavaPump = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        lavaPump.storeFluid(FluidType.LAVA)
        val waterPump = FluidPump(MockServer.createLocation(0.0, 64.0, 4.0))
        waterPump.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(MockServer.createLocation(0.0, 64.0, it.toDouble())) }
        registry.track(lavaPump, "atlas:fluid_pump")
        registry.track(waterPump, "atlas:fluid_pump")
        for (pipe in pipes) registry.track(pipe, "atlas:fluid_pipe")

        // SOUTH is +Z, so this is the arm pointing across the seam at the water side
        assertFalse(BlockFace.SOUTH in pipes[1].connections(), "the lava pipe should not reach across")
        assertFalse(BlockFace.NORTH in pipes[2].connections(), "nor the water pipe back")
        assertTrue(BlockFace.NORTH in pipes[1].connections(), "but it still joins its own run")
    }

    @Test
    fun `an unfed run is still one network`() {
        val pipes = (0..3).map { FluidPipe(MockServer.createLocation(0.0, 64.0, it.toDouble())) }
        for (pipe in pipes) registry.track(pipe, "atlas:fluid_pipe")

        // nothing labels these pipes, so they must not fragment into one network each
        assertEquals(4, FluidNetworks.networkFor(pipes[0]).pipes.size)
    }

    @Test
    fun `two runs on the same fluid still join`() {
        val left = FluidPump(MockServer.createLocation(0.0, 64.0, -1.0))
        left.storeFluid(FluidType.WATER)
        val right = FluidPump(MockServer.createLocation(0.0, 64.0, 4.0))
        right.storeFluid(FluidType.WATER)

        val pipes = (0..3).map { FluidPipe(MockServer.createLocation(0.0, 64.0, it.toDouble())) }
        registry.track(left, "atlas:fluid_pump")
        registry.track(right, "atlas:fluid_pump")
        for (pipe in pipes) registry.track(pipe, "atlas:fluid_pipe")

        assertEquals(4, FluidNetworks.networkFor(pipes[0]).pipes.size, "same fluid, so one run")
    }

    @Test
    fun `pipe does nothing when source has no fluid`() {
        val pipe =
            FluidPipe(MockServer.createLocation(0.0, 64.0, 0.0))
        val sourcePipe =
            FluidPipe(MockServer.createLocation(0.0, 64.0, -1.0))

        registry.track(
            pipe,
            "atlas:fluid_pipe",
        )
        registry.track(
            sourcePipe,
            "atlas:fluid_pipe",
        )

        pipe.fluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }

    @Test
    fun `pipe does nothing when no fluid block behind it`() {
        val pipe =
            FluidPipe(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(
            pipe,
            "atlas:fluid_pipe",
        )

        pipe.fluidUpdate()
        assertEquals(FluidType.NONE, pipe.storedFluid)
    }
}
