package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.deposit.Ore
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DepositReadoutTest {
    private fun readout(ore: Map<Ore, Int>?) = PlainTextComponentSerializer.plainText().serialize(DepositReadout.readout(ore))

    private fun chunkHolding(vararg ore: Pair<Ore, Int>) = Ore.entries.associateWith { 0 } + ore

    @Test
    fun `an unsurveyed chunk says it is being surveyed`() {
        assertEquals("Surveying this chunk...", readout(null))
    }

    @Test
    fun `a chunk with no ore says so`() {
        assertEquals("No ore deposits in this chunk", readout(chunkHolding()))
    }

    @Test
    fun `only the ores a chunk holds are listed, each with its purity`() {
        val ore = chunkHolding(Ore.COAL to 300, Ore.IRON to 70, Ore.DIAMOND to 5)

        assertEquals("Deposits: Coal Pure · Iron Normal · Diamond Poor", readout(ore))
    }
}
