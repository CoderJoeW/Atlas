package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace

class NetheriteMine(location: Location, facing: BlockFace = BlockFace.NORTH) :
    Mine(location, maxStorage = 100, facing = facing) {
    companion object {
        const val BLOCK_ID = "atlas:netherite_mine"
        const val POWER_PER_HAUL = 30
        const val CYCLE_TICKS = 200L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Netherite Mine",
                description = "Mine - consumes $POWER_PER_HAUL power every 10s \u2192 1 ancient debris",
                // The shaft mouth is turned to look back at the player who placed it.
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, face -> NetheriteMine(loc, face) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val updateIntervalTicks: Long = CYCLE_TICKS
    override val powerPerHaul: Int = POWER_PER_HAUL
    override val output: Material = Material.ANCIENT_DEBRIS
}
