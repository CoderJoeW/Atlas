package com.coderjoe.atlas.block.power.mine

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.PlacementType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class DiamondMine(location: Location, facing: BlockFace = BlockFace.NORTH) :
    Mine(location, maxStorage = 60, facing = facing) {
    companion object {
        const val BLOCK_ID = "atlas:diamond_mine"
        const val POWER_PER_HAUL = 18
        const val CYCLE_TICKS = 800L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Diamond Mine",
                description = "Mine - consumes $POWER_PER_HAUL power every 40s \u2192 1 diamond",
                // The shaft mouth is turned to look back at the player who placed it.
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, face -> DiamondMine(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val cycleTicks: Long = CYCLE_TICKS
    override val powerPerHaul: Int = POWER_PER_HAUL
    override val output: Material = Material.DIAMOND
}
