package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.deposit.Ore
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.World.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DepositReadoutTest {
    private val plain = PlainTextComponentSerializer.plainText()

    private fun readout(
        ore: Map<Ore, Int>?,
        dimension: Environment = Environment.NORMAL,
    ) = DepositReadout.readout(ore, dimension).map { listOf(plain.serialize(it.label), plain.serialize(it.value)).joinToString(" ").trim() }

    private fun chunkHolding(vararg ore: Pair<Ore, Int>) = Ore.entries.associateWith { 0 } + ore

    @Test
    fun `an unsurveyed chunk says it is being surveyed`() {
        assertEquals(listOf("Surveying..."), readout(null))
    }

    @Test
    fun `every overworld ore is stacked in order, each with its purity`() {
        val ore = chunkHolding(Ore.COAL to 300, Ore.IRON to 70, Ore.DIAMOND to 5)

        assertEquals(
            listOf(
                "Coal Pure",
                "Copper Barren",
                "Iron Normal",
                "Redstone Barren",
                "Lapis Barren",
                "Amethyst Barren",
                "Gold Barren",
                "Emerald Barren",
                "Diamond Poor",
            ),
            readout(ore),
        )
    }

    @Test
    fun `the nether lists only the ores it generates`() {
        val ore = chunkHolding(Ore.NETHER_QUARTZ to 160, Ore.ANCIENT_DEBRIS to 2)

        assertEquals(listOf("Nether Quartz Rich", "Ancient Debris Normal"), readout(ore, Environment.NETHER))
    }

    @Test
    fun `a dimension with no ore says so`() {
        assertEquals(listOf("No ore here"), readout(chunkHolding(), Environment.THE_END))
    }

    @Test
    fun `a barren deposit is dimmed rather than flagged red`() {
        val coal = DepositReadout.readout(chunkHolding(), Environment.NORMAL).first()

        assertEquals(NamedTextColor.DARK_GRAY, coal.value.color())
    }
}
