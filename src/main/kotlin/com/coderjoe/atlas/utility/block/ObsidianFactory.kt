package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.PlacementType
import org.bukkit.Location
import org.bukkit.Material

class ObsidianFactory(location: Location) : MaterialFactory(location, maxStorage = 50) {
    companion object {
        const val BLOCK_ID = "atlas:obsidian_factory"
        const val POWER_COST = 25

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Obsidian Factory",
                description = "Machine - consumes $POWER_COST power + water + lava → obsidian",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> ObsidianFactory(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val powerCost: Int = POWER_COST
    override val outputMaterial: Material = Material.OBSIDIAN
}
