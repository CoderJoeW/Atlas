package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Item
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack

class AutoSmelter(location: Location, facing: BlockFace = BlockFace.NORTH) : PowerBlock(location, maxStorage = 2) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L
    override val baseBlockId: String = BLOCK_ID

    var direction: BlockFace = if (facing == BlockFace.SELF) BlockFace.NORTH else facing

    override val facing: BlockFace get() = direction

    companion object {
        const val BLOCK_ID = "atlas:auto_smelter"
        const val POWER_PER_SMELT = 2
        private const val MOVE_DISTANCE = 1.0

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Auto Smelter",
                description = "Smelts items passing through, consumes 2 power per item",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, face -> AutoSmelter(loc, face) },
            )

        fun getSmeltingResult(input: ItemStack): ItemStack? {
            return try {
                Bukkit.recipeIterator().asSequence()
                    .filterIsInstance<FurnaceRecipe>()
                    .find { it.inputChoice.test(input) }
                    ?.result
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        val world = location.world ?: return

        val scanCenter = location.clone().add(0.5, 0.75, 0.5)
        val nearbyItems =
            world.getNearbyEntities(scanCenter, 0.5, 0.75, 0.5)
                .filterIsInstance<Item>()

        if (nearbyItems.isEmpty()) return

        val dx = direction.direction.x * MOVE_DISTANCE
        val dz = direction.direction.z * MOVE_DISTANCE

        for (item in nearbyItems) {
            if (currentPower >= POWER_PER_SMELT) {
                val result = getSmeltingResult(item.itemStack)
                if (result != null) {
                    removePower(POWER_PER_SMELT)
                    val smeltedStack = result.clone()
                    smeltedStack.amount = item.itemStack.amount
                    item.itemStack = smeltedStack
                }
            }

            item.teleportAsync(item.location.add(dx, 0.0, dz))
        }

        updatePoweredState()
    }

    private fun updatePoweredState() {
        val shouldBePowered = hasPower()
        CraftEngineHelper.setBooleanProperty(location, "powered", shouldBePowered)
    }
}
