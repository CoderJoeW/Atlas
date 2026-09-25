package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Gauge
import com.coderjoe.atlas.block.Inspection
import com.coderjoe.atlas.block.StatusLine
import com.coderjoe.atlas.block.Tone
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

    private fun cable(y: Double): PowerCable =
        PowerCable(MockServer.createLocation(0.0, y, 0.0)).also { registry.track(it, "atlas:power_cable") }

    private fun Inspection.diagnosis(): StatusLine = lines.last()

    @Test
    fun `a run with a producer and a consumer reports as flowing`() {
        val cable = cable(64.0)
        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        registry.track(panel, "atlas:small_solar_panel")
        registry.track(SmallBattery(MockServer.createLocation(0.0, 63.0, 0.0)), "atlas:small_battery")

        val inspection = cable.inspect()

        assertEquals(StatusLine("Power is flowing", Tone.GOOD), inspection.diagnosis())
        assertEquals(listOf(Gauge("Stored", 3, 54)), inspection.gauges)
    }

    @Test
    fun `a run with no generator says so`() {
        val cable = cable(64.0)
        registry.track(SmallBattery(MockServer.createLocation(0.0, 63.0, 0.0)), "atlas:small_battery")

        assertEquals(StatusLine("No generator is feeding this run", Tone.FAULT), cable.inspect().diagnosis())
    }

    @Test
    fun `a run with nothing that can take power says so`() {
        val cable = cable(64.0)
        val panel = SmallSolarPanel(MockServer.createLocation(0.0, 65.0, 0.0))
        panel.currentPower = 3
        registry.track(panel, "atlas:small_solar_panel")

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
