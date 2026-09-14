package com.coderjoe.atlas.block.power.mine

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.PlacementType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class EmeraldMine(location: Location, facing: BlockFace = BlockFace.NORTH) :
    Mine(location, maxStorage = 50, facing = facing) {
    companion object {
        const val BLOCK_ID = "atlas:emerald_mine"
        const val POWER_PER_HAUL = 14
        const val CYCLE_TICKS = 600L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Emerald Mine",
                description = "Mine - consumes $POWER_PER_HAUL power every 30s \u2192 1 emerald",
                // The shaft mouth is turned to look back at the player who placed it.
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, face -> EmeraldMine(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val cycleTicks: Long = CYCLE_TICKS
    override val powerPerHaul: Int = POWER_PER_HAUL
    override val output: Material = Material.EMERALD
}
