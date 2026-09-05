package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class CoalMine(location: Location, facing: BlockFace = BlockFace.NORTH) :
    Mine(location, maxStorage = 10, facing = facing) {
    companion object {
        const val BLOCK_ID = "atlas:coal_mine"
        const val POWER_PER_HAUL = 2
        const val CYCLE_TICKS = 40L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Coal Mine",
                description = "Mine - consumes $POWER_PER_HAUL power every 2s \u2192 1 coal",
                // The shaft mouth is turned to look back at the player who placed it.
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, face -> CoalMine(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val updateIntervalTicks: Long = CYCLE_TICKS
    override val powerPerHaul: Int = POWER_PER_HAUL
    override val output: Material = Material.COAL
}
