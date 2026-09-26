package com.coderjoe.atlas.block.deposit

import org.bukkit.Material
import org.bukkit.World.Environment
import org.bukkit.World.Environment.NETHER
import org.bukkit.World.Environment.NORMAL

/**
 * An ore a mine can dig, the [dimension] that generates it, every block it generates as, and the
 * counts a chunk needs to reach each [Purity].
 *
 * Nether gold ore is left out of [GOLD] on purpose: it yields nuggets, not the raw gold a gold
 * mine hauls, so a Nether chunk holds no gold deposit worth mining.
 *
 * The thresholds are the 25th, 75th and 95th percentiles of freshly generated chunks that hold
 * the ore at all, surveyed in `docs/design/ore-purity/README.md`. Emerald's are rounded out of a
 * small sample, since only about one Overworld chunk in forty holds any.
 *
 * An amethyst deposit is a geode's budding amethyst, the only block in it that grows shards.
 */
enum class Ore(
    val displayName: String,
    val dimension: Environment,
    normalAt: Int,
    richAt: Int,
    pureAt: Int,
    vararg blocks: Material,
) {
    COAL("Coal", NORMAL, 43, 121, 241, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
    COPPER("Copper", NORMAL, 60, 116, 264, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER_BLOCK),
    IRON("Iron", NORMAL, 65, 84, 102, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON_BLOCK),
    REDSTONE("Redstone", NORMAL, 29, 43, 52, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
    LAPIS("Lapis", NORMAL, 19, 28, 34, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
    AMETHYST("Amethyst", NORMAL, 4, 24, 56, Material.BUDDING_AMETHYST),
    GOLD("Gold", NORMAL, 21, 31, 40, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE),
    NETHER_QUARTZ("Nether Quartz", NETHER, 62, 155, 223, Material.NETHER_QUARTZ_ORE),
    EMERALD("Emerald", NORMAL, 2, 5, 10, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
    DIAMOND("Diamond", NORMAL, 18, 29, 39, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
    ANCIENT_DEBRIS("Ancient Debris", NETHER, 2, 3, 4, Material.ANCIENT_DEBRIS),
    ;

    val blocks: Set<Material> = blocks.toSet()

    private val floors = listOf(Purity.PURE to pureAt, Purity.RICH to richAt, Purity.NORMAL to normalAt, Purity.POOR to 1)

    fun purityOf(count: Int): Purity = floors.firstOrNull { (_, floor) -> count >= floor }?.first ?: Purity.BARREN

    companion object {
        /** Indexed by [Material.ordinal], since a chunk survey asks this for every block in the chunk. */
        private val byBlock: Array<Ore?> =
            arrayOfNulls<Ore>(Material.entries.size).also { table ->
                entries.forEach { ore -> ore.blocks.forEach { table[it.ordinal] = ore } }
            }

        fun of(block: Material): Ore? = byBlock[block.ordinal]
    }
}
