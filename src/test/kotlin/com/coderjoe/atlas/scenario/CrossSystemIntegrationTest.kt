package com.coderjoe.atlas.scenario

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.fluid.FluidContainer
import com.coderjoe.atlas.block.fluid.FluidPipe
import com.coderjoe.atlas.block.fluid.FluidPump
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.block.power.PowerCable
import com.coderjoe.atlas.block.power.SmallSolarPanel
import com.coderjoe.atlas.testing.MockServer
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CrossSystemIntegrationTest {
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
    fun `pump with adjacent powered block extracts fluid`() {
        every { MockServer.world.time } returns 6000L

        // Solar panel at (1,64,0)
        val solar = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        registry.track(solar, "atlas:lava_generator")

        // Pump at (0,64,0)
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(pump, "atlas:fluid_pump")

        // Water cauldron to the NORTH
        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every { MockServer.world.getBlockAt(0, 64, -1) } returns cauldronBlock

        // Other directions are air
        for (face in listOf(BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { MockServer.world.getBlockAt(offset.blockX, 64 + offset.blockY, offset.blockZ) } returns block
        }

        // the generator pushes into the pump; the pump never reaches out for power itself
        solar.powerUpdate()
        assertEquals(1, pump.storedPower, "the generator should have fed the pump")

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
        assertEquals(FluidType.WATER, pump.storedFluid)
    }

    @Test
    fun `pump with no powered neighbors gets NO_POWER`() {
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(pump, "atlas:fluid_pump")

        // Water cauldron to the NORTH
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { MockServer.world.getBlockAt(0, 64, -1) } returns cauldronBlock

        for (face in listOf(BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { MockServer.world.getBlockAt(offset.blockX, 64 + offset.blockY, offset.blockZ) } returns block
        }

        pump.fluidUpdate()
        assertEquals(FluidPump.PumpStatus.NO_POWER, pump.pumpStatus)
    }

    @Test
    fun `full end-to-end - solar to cable near pump, pump extracts, pipe transports`() {
        every { MockServer.world.time } returns 6000L

        // Solar at (0,65,1) - generates power, outputs through its base pad (DOWN)
        val solar = SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 1.0))
        registry.track(solar, "atlas:small_solar_panel")

        // Cable at (0,64,1) - joins the panel above to the pump below, no facing to set
        val cable = PowerCable(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(cable, "atlas:power_cable")

        // Pump at (0,63,1) - directly below the cable, in its output direction
        val pump = FluidPump(MockServer.createLocation(0.0, 63.0, 1.0))
        registry.track(pump, "atlas:fluid_pump")

        // Water cauldron at (0,63,2) = SOUTH of pump
        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every { MockServer.world.getBlockAt(0, 63, 2) } returns cauldronBlock

        // Other blocks around the pump are air (UP holds the cable, SOUTH the cauldron)
        for (face in listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { MockServer.world.getBlockAt(0 + offset.blockX, 63 + offset.blockY, 1 + offset.blockZ) } returns block
        }

        // Pipe at (-1,63,1) facing WEST, pulling from the pump behind it (EAST = x+1)
        val pipe = FluidPipe(MockServer.createLocation(-1.0, 63.0, 1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        // Step 1: solar generates 1 and holds it - a cable stores nothing, so there is nowhere
        // for the panel to push it yet
        solar.ticksSinceGeneration = SmallSolarPanel.GENERATION_INTERVAL_TICKS
        solar.powerUpdate()
        assertEquals(1, solar.currentPower)
        assertTrue(cable.canSupplyPower())

        // Step 2: the run ticks and drives the panel's charge into the pump on its edge. The
        // pump's buffer has room for the unit, so the panel empties in one go.
        cable.powerUpdate()
        assertEquals(1, pump.storedPower, "the run should have fed the pump")
        assertEquals(0, solar.currentPower, "and taken it off the panel")

        // Step 3: pump spends it lifting water out of the cauldron
        pump.fluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)

        assertTrue(pump.hasFluid(), "Pump should have extracted fluid using power from solar->cable chain")
    }

    @Test
    fun `pump extracts lava from lava cauldron with power`() {
        val solar = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        registry.track(solar, "atlas:lava_generator")

        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(pump, "atlas:fluid_pump")

        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.LAVA_CAULDRON
        every { MockServer.world.getBlockAt(0, 64, -1) } returns cauldronBlock

        for (face in listOf(BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { MockServer.world.getBlockAt(offset.blockX, 64 + offset.blockY, offset.blockZ) } returns block
        }

        solar.powerUpdate()

        pump.fluidUpdate()
        assertEquals(FluidType.LAVA, pump.storedFluid)
        assertEquals(FluidPump.PumpStatus.EXTRACTING, pump.pumpStatus)
    }

    @Test
    fun `complete pipeline - pump extracts and pipe receives fluid`() {
        every { MockServer.world.time } returns 6000L

        // Solar at (1,64,0)
        val solar = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        solar.currentPower = 1
        registry.track(solar, "atlas:lava_generator")

        // Pump at (0,64,0)
        val pump = FluidPump(MockServer.createLocation(0.0, 64.0, 0.0))
        registry.track(pump, "atlas:fluid_pump")

        // Water cauldron to the NORTH of pump
        val levelled = mockk<Levelled>(relaxed = true)
        every { levelled.level } returns 3
        val cauldronBlock = mockk<Block>(relaxed = true)
        every { cauldronBlock.type } returns Material.WATER_CAULDRON
        every { cauldronBlock.blockData } returns levelled
        every { MockServer.world.getBlockAt(0, 64, -1) } returns cauldronBlock

        for (face in listOf(BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
            val offset = face.direction
            val block = mockk<Block>(relaxed = true)
            every { block.type } returns Material.AIR
            every { MockServer.world.getBlockAt(offset.blockX, 64 + offset.blockY, offset.blockZ) } returns block
        }

        // Step 1: the generator feeds the pump, and the pump lifts a unit out of the cauldron
        solar.powerUpdate()
        pump.fluidUpdate()
        assertEquals(FluidType.WATER, pump.storedFluid)

        // A pipe holds nothing, so the run needs somewhere to put the water: pump -> pipe -> tank.
        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, 2.0))
        registry.track(tank, "atlas:fluid_container")

        // Step 2: the pump hands its unit to the pipe, and the run carries it to the tank
        pump.fluidUpdate()
        assertEquals(FluidType.WATER, tank.storedFluid)
        assertEquals(FluidType.NONE, pump.storedFluid)
    }
}
