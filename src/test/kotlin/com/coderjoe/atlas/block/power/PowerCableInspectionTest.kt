package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Gauge
import com.coderjoe.atlas.block.Inspection
import com.coderjoe.atlas.block.StatusLine
import com.coderjoe.atlas.block.Tone
import com.coderjoe.atlas.testing.Blocks.placedIn
import com.coderjoe.atlas.testing.MockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** A cable stores nothing, so what it reports is the run it belongs to. */
class PowerCableInspectionTest {
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

    private fun cable(y: Double): PowerCable = PowerCable(MockServer.createLocation(0.0, y, 0.0)).placedIn(registry)

    /** A charged solar panel on top of the cable at y 64. */
    private fun producer(): SmallSolarPanel =
        SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 0.0)).placedIn(registry).also { it.currentPower = 3 }

    /** An empty battery under the cable at y 64. */
    private fun battery(): SmallBattery = SmallBattery(MockServer.createLocation(0.0, 63.0, 0.0)).placedIn(registry)

    private fun Inspection.diagnosis(): StatusLine = lines.last()

    @Test
    fun `a run with a producer and a consumer reports as flowing`() {
        val cable = cable(64.0)
        producer()
        battery()

        val inspection = cable.inspect()

        assertEquals(StatusLine("Power is flowing", Tone.GOOD), inspection.diagnosis())
        assertEquals(listOf(Gauge("Stored", 3, 54)), inspection.gauges)
    }

    @Test
    fun `a run with no generator says so`() {
        val cable = cable(64.0)
        battery()

        assertEquals(StatusLine("No generator is feeding this run", Tone.FAULT), cable.inspect().diagnosis())
    }

    @Test
    fun `a run with nothing that can take power says so`() {
        val cable = cable(64.0)
        producer()

        assertEquals(StatusLine("Nothing on this run can take power", Tone.FAULT), cable.inspect().diagnosis())
    }

    /** No terminals means no capacity, and a 0/0 gauge would only read as an empty red bar. */
    @Test
    fun `an empty run says nothing is attached and shows no gauge`() {
        val inspection = cable(64.0).inspect()

        assertEquals(StatusLine("Nothing is attached to this run yet", Tone.WARNING), inspection.diagnosis())
        assertTrue(inspection.gauges.isEmpty())
    }

    @Test
    fun `every cable in a run reports the whole run`() {
        val top = cable(64.0)
        cable(63.0)

        assertEquals(StatusLine("Cable: 2 blocks"), top.inspect().lines.first())
    }
}
