package com.coderjoe.atlas.block.power.mine

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.PlacementType
import org.bukkit.Material

/**
 * The seven mines, as data: every tier is the same [Mine] with different numbers. The rarer the
 * ore, the slower and thirstier the rig and the more charge it can bank.
 *
 * Adding a tier is one entry here plus its CraftEngine config. [blockId] is persisted and cached
 * by CraftEngine's state pools, so an existing tier's id must never change.
 */
enum class MineTier(
    val blockId: String,
    val displayName: String,
    /** What a completed bore drops. */
    val output: Material,
    /** Power drawn per haul. Charged the moment a haul is committed, not when it completes. */
    val powerPerHaul: Int,
    /** How long a committed haul takes to finish, no matter how much power is banked. */
    val cycleTicks: Long,
    val maxStorage: Int,
) {
    COAL("atlas:coal_mine", "Coal Mine", Material.COAL, 2, 200L, 10),
    IRON("atlas:iron_mine", "Iron Mine", Material.RAW_IRON, 5, 300L, 20),
    REDSTONE("atlas:redstone_mine", "Redstone Mine", Material.REDSTONE, 5, 300L, 20),
    GOLD("atlas:gold_mine", "Gold Mine", Material.RAW_GOLD, 8, 400L, 30),
    EMERALD("atlas:emerald_mine", "Emerald Mine", Material.EMERALD, 14, 600L, 50),
    DIAMOND("atlas:diamond_mine", "Diamond Mine", Material.DIAMOND, 18, 800L, 60),
    NETHERITE("atlas:netherite_mine", "Netherite Mine", Material.ANCIENT_DEBRIS, 30, 1000L, 100),
    ;

    val descriptor: BlockDescriptor =
        BlockDescriptor(
            baseBlockId = blockId,
            displayName = displayName,
            description =
                "Mine - consumes $powerPerHaul power every ${cycleTicks / 20}s → " +
                    "1 ${output.name.lowercase().replace('_', ' ')}",
            // The shaft mouth is turned to look back at the player who placed it.
            placementType = PlacementType.DIRECTIONAL_OPPOSITE,
            constructor = { loc, face -> Mine(loc, this, face) },
        )
}
