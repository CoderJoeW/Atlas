package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class GoldMine(location: Location, facing: BlockFace = BlockFace.NORTH) :
    Mine(location, maxStorage = 30, facing = facing) {
    companion object {
        const val BLOCK_ID = "atlas:gold_mine"
        const val POWER_PER_HAUL = 8
        const val CYCLE_TICKS = 80L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Gold Mine",
                description = "Mine - consumes $POWER_PER_HAUL power every 4s \u2192 1 raw gold",
                // The shaft mouth is turned to look back at the player who placed it.
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, face -> GoldMine(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val updateIntervalTicks: Long = CYCLE_TICKS
    override val powerPerHaul: Int = POWER_PER_HAUL
    override val output: Material = Material.RAW_GOLD
}
