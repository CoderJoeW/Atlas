package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.power.LavaGenerator
import com.coderjoe.atlas.testing.AtlasPaths.config
import com.coderjoe.atlas.testing.MockServer
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The pump is a pure source: it fills itself from the world and hands fluid out of any face.
 *
 * It used to answer only for the face opposite whatever side the source happened to be on, which
 * gave it an output port that was invisible, unchosen, and liable to move on its own.
 */
class FluidPumpTest {
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

    private fun pump(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
        fluid: FluidType = FluidType.NONE,
    ): FluidPump =
        FluidPump(MockServer.createLocation(x, y, z)).also {
            if (fluid != FluidType.NONE) it.storeFluid(fluid)
            registry.track(it, "atlas:fluid_pump")
        }

    @Test
    fun `a loaded pump gives fluid out of every face`() {
        val pump = pump(fluid = FluidType.WATER)

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertTrue(pump.canProvideFluid(face), "should give toward $face")
        }
    }

    @Test
    fun `an empty pump gives nothing`() {
        val pump = pump()

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertFalse(pump.canProvideFluid(face), "nothing to give toward $face")
        }
    }

    @Test
    fun `a pump never takes fluid in`() {
        val pump = pump()

        for (face in AtlasBlock.ADJACENT_FACES) {
            assertFalse(pump.canAcceptFluid(face), "a pump only ever sources, got an inlet on $face")
        }
    }

    @Test
    fun `ports show on the faces something will take fluid from`() {
        val pump = pump(fluid = FluidType.WATER)

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(pipe, "atlas:fluid_pipe")

        // a tank takes fluid in on every side now, so the face that stays plain casing is one
        // against a tank with no room left in it
        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, -1.0))
        repeat(FluidContainer.MAX_CAPACITY) { tank.storeFluid(FluidType.WATER) }
        registry.track(tank, "atlas:fluid_container")

        val ports = pump.connections()

        assertTrue(BlockFace.SOUTH in ports, "the pipe should show a port")
        assertFalse(BlockFace.NORTH in ports, "a tank with no room should not")
    }

    @Test
    fun `a working pump names the fluid it is handling`() {
        val water = pump(fluid = FluidType.WATER).also { it.pumpStatus = FluidPump.PumpStatus.IDLE }
        val lava = pump(x = 1.0, fluid = FluidType.LAVA).also { it.pumpStatus = FluidPump.PumpStatus.EXTRACTING }

        assertEquals("idle_water", water.statusProperty())
        assertEquals("extracting_lava", lava.statusProperty())
    }

    @Test
    fun `a pump with nothing in hand has no fluid to name`() {
        assertEquals("no_source", pump().statusProperty())
    }

    @Test
    fun `every status the pump can render is a value the config declares`() {
        val config = config("fluid_pump.yml").readText()
        val declared =
            Regex("""values: \[([^\]]+)]""")
                .findAll(config)
                .flatMap { it.groupValues[1].split(",") }
                .map { it.trim() }
                .toSet()

        // Every combination the block can actually set, walked the same way the renderer builds it
        val rendered = mutableSetOf<String>()
        for (status in FluidPump.PumpStatus.entries) {
            for (fluid in listOf(FluidType.NONE, FluidType.WATER, FluidType.LAVA)) {
                val subject = pump(x = 5.0, fluid = fluid)
                subject.pumpStatus = status
                rendered += subject.statusProperty()
            }
        }

        val missing = rendered - declared
        assertTrue(missing.isEmpty(), "the pump can render states the config does not declare: $missing")
    }

    @Test
    fun `a pump never takes power from a generator beside it`() {
        val pump = pump()

        val generator = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        generator.currentPower = 5
        registry.track(generator, "atlas:lava_generator")

        pump.fluidUpdate()

        assertEquals(5, generator.currentPower, "the pump must not reach into a generator")
        assertEquals(0, pump.storedPower)
    }

    @Test
    fun `a generator pushes power into the pump beside it`() {
        val pump = pump()

        val generator = LavaGenerator(MockServer.createLocation(1.0, 64.0, 0.0))
        generator.currentPower = 5
        registry.track(generator, "atlas:lava_generator")

        generator.powerUpdate()

        assertTrue(pump.storedPower > 0, "the generator should have fed the pump")
        assertTrue(generator.currentPower < 5, "and spent what it handed over")
    }

    @Test
    fun `the pump fills its buffer and no further`() {
        val pump = pump()

        assertEquals(FluidPump.POWER_CAPACITY, pump.acceptPower(BlockFace.NORTH, 99))
        assertFalse(pump.wantsPower())
        assertEquals(0, pump.acceptPower(BlockFace.NORTH, 1), "a full pump takes nothing more")
    }

    @Test
    fun `the pump hands its fluid to the pipe beside it`() {
        val pump = pump(fluid = FluidType.WATER)

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(pipe, "atlas:fluid_pipe")
        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, 2.0))
        registry.track(tank, "atlas:fluid_container")

        pump.fluidUpdate()

        assertEquals(FluidType.NONE, pump.storedFluid, "the pump pushes its unit out itself")
        assertEquals(FluidType.WATER, tank.storedFluid)
    }

    @Test
    fun `a pipe run does not drain the pump behind its back`() {
        val pump = pump(fluid = FluidType.WATER)

        val pipe = FluidPipe(MockServer.createLocation(0.0, 64.0, 1.0))
        registry.track(pipe, "atlas:fluid_pipe")
        val tank = FluidContainer(MockServer.createLocation(0.0, 64.0, 2.0))
        registry.track(tank, "atlas:fluid_container")

        // ticking the run must not move anything: the pump pushes, it is not pulled from
        pipe.fluidUpdate()

        assertEquals(FluidType.WATER, pump.storedFluid, "the run must leave the pump alone")
        assertEquals(FluidType.NONE, tank.storedFluid)
    }

    @Test
    fun `a pump alone has no ports`() {
        assertEquals(emptySet<BlockFace>(), pump().connections())
    }

    @Test
    fun `status starts with no source`() {
        assertEquals(FluidPump.PumpStatus.NO_SOURCE, pump().pumpStatus)
    }

    @Test
    fun `descriptor registers a single block id`() {
        val descriptor = FluidPump.descriptor

        assertEquals("atlas:fluid_pump", descriptor.baseBlockId)
        // connections and status are block state properties now, not separate block ids
        assertTrue(descriptor.additionalBlockIds.isEmpty())
    }
}
