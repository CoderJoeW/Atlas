package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

class SoftTouchDrill(location: Location, facing: BlockFace? = null) : SmallDrill(location, facing, maxStorage = 40) {
    override val powerCost: Int = 20

    companion object {
        const val BLOCK_ID = "atlas:soft_touch_drill"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Soft Touch Drill",
                description = "Machine - consumes 20 power/s, drops blocks in original form",
                placementType = PlacementType.DIRECTIONAL_OPPOSITE,
                constructor = { loc, facing -> SoftTouchDrill(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun getBlockDrops(block: org.bukkit.block.Block): Collection<ItemStack> {
        return listOf(ItemStack(block.type))
    }
}
