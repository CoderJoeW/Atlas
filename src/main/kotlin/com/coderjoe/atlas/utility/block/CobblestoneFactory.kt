package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.Material

class CobblestoneFactory(location: Location) : MaterialFactory(location, maxStorage = 4) {
    companion object {
        const val BLOCK_ID = "atlas:cobblestone_factory"
        const val POWER_COST = 2

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Cobblestone Factory",
                description = "Machine - consumes $POWER_COST power + water + lava → cobblestone",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> CobblestoneFactory(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID
    override val activeBlockId: String = BLOCK_ID
    override val powerCost: Int = POWER_COST
    override val outputMaterial: Material = Material.COBBLESTONE

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        super.powerUpdate()
        updatePoweredState()
    }
}
