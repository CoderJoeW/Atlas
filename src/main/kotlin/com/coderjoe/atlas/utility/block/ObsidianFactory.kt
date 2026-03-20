package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.Material

class ObsidianFactory(location: Location) : MaterialFactory(location, maxStorage = 100) {
    companion object {
        const val BLOCK_ID = "atlas:obsidian_factory"
        const val BLOCK_ID_ACTIVE = "atlas:obsidian_factory_active"
        const val POWER_COST = 100

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Obsidian Factory",
                description = "Machine - consumes $POWER_COST power + water + lava → obsidian",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, _ -> ObsidianFactory(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val powerCost: Int = POWER_COST
    override val outputMaterial: Material = Material.OBSIDIAN

    override fun getVisualStateBlockId(): String =
        when {
            currentPower >= POWER_COST -> BLOCK_ID_ACTIVE
            else -> BLOCK_ID
        }
}
