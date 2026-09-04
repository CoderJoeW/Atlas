package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.TestHelper
import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
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
    private lateinit var registry: FluidBlockRegistry

    @BeforeEach
    fun setup() {
        TestHelper.setup()
        registry = FluidBlockRegistry(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    private fun pump(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
        fluid: FluidType = FluidType.NONE,
    ): FluidPump =
        FluidPump(TestHelper.createLocation(x, y, z)).also {
            if (fluid != FluidType.NONE) it.storeFluid(fluid)
            TestHelper.addToRegistry(registry, it, "atlas:fluid_pump")
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

        val pipe = FluidPipe(TestHelper.createLocation(0.0, 64.0, 1.0))
        TestHelper.addToRegistry(registry, pipe, "atlas:fluid_pipe")

        // a container takes fluid in on the face behind it, so one facing south presents its back
        // to open air, not to the pump - that face stays plain casing
        val tank = FluidContainer(TestHelper.createLocation(0.0, 64.0, -1.0), BlockFace.SOUTH)
        TestHelper.addToRegistry(registry, tank, "atlas:fluid_container")

        val ports = pump.connections()

        assertTrue(BlockFace.SOUTH in ports, "the pipe should show a port")
        assertFalse(BlockFace.NORTH in ports, "a container facing away should not")
    }

    private fun statusProperty(pump: FluidPump): String {
        val method = FluidPump::class.java.getDeclaredMethod("statusProperty")
        method.isAccessible = true
        return method.invoke(pump) as String
    }

    private fun setStatus(
        pump: FluidPump,
        status: FluidPump.PumpStatus,
    ) {
        val field = FluidPump::class.java.getDeclaredField("pumpStatus")
        field.isAccessible = true
        field.set(pump, status)
    }

    @Test
    fun `a working pump names the fluid it is handling`() {
        val water = pump(fluid = FluidType.WATER).also { setStatus(it, FluidPump.PumpStatus.IDLE) }
        val lava = pump(x = 1.0, fluid = FluidType.LAVA).also { setStatus(it, FluidPump.PumpStatus.EXTRACTING) }

        assertEquals("idle_water", statusProperty(water))
        assertEquals("extracting_lava", statusProperty(lava))
    }

    @Test
    fun `a pump with nothing in hand has no fluid to name`() {
        assertEquals("no_source", statusProperty(pump()))
    }

    @Test
    fun `every status the pump can render is a value the config declares`() {
        val config = java.io.File("src/main/resources/atlas/configuration/fluid_pump.yml").readText()
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
                setStatus(subject, status)
                rendered += statusProperty(subject)
            }
        }

        val missing = rendered - declared
        assertTrue(missing.isEmpty(), "the pump can render states the config does not declare: $missing")
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
