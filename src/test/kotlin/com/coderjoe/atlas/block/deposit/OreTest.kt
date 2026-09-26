package com.coderjoe.atlas.block.deposit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OreTest {
    @Test
    fun `no block belongs to two ores`() {
        val blocks = Ore.entries.flatMap { it.blocks }

        assertEquals(blocks.size, blocks.toSet().size)
    }

    @Test
    fun `every block resolves back to its own ore`() {
        Ore.entries.forEach { ore -> ore.blocks.forEach { assertEquals(ore, Ore.of(it), "$it") } }
    }

    @Test
    fun `a chunk holding none of an ore is barren and one holding any is not`() {
        Ore.entries.forEach {
            assertEquals(Purity.BARREN, it.purityOf(0), "$it")
            assertTrue(it.purityOf(1) > Purity.BARREN, "$it")
        }
    }

    @Test
    fun `purity never falls as the count rises and every grade is reachable`() {
        Ore.entries.forEach { ore ->
            val grades = (0..1000).map { ore.purityOf(it) }

            assertEquals(grades.sorted(), grades, "$ore")
            assertEquals(Purity.entries.toSet(), grades.toSet(), "$ore")
        }
    }

    @Test
    fun `each grade starts exactly at its threshold`() {
        assertEquals(Purity.POOR, Ore.DIAMOND.purityOf(17))
        assertEquals(Purity.NORMAL, Ore.DIAMOND.purityOf(18))
        assertEquals(Purity.RICH, Ore.DIAMOND.purityOf(29))
        assertEquals(Purity.PURE, Ore.DIAMOND.purityOf(39))
    }
}
