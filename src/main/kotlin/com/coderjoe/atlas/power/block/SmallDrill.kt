package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material

class SmallDrill(location: Location) : PowerBlock(location, maxStorage = 10) {

    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "small_drill"
    }

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        // Pull power from adjacent blocks
        if (canAcceptPower()) {
            val registry = PowerBlockRegistry.instance ?: return
            val neighbors = registry.getAdjacentPowerBlocks(location)
            for (neighbor in neighbors) {
                if (!canAcceptPower()) break
                if (neighbor.hasPower()) {
                    val pulled = neighbor.removePower(1)
                    if (pulled > 0) {
                        addPower(pulled)
                    }
                }
            }
        }

        if (currentPower < 10) return

        val world = location.world ?: return

        for (y in (location.blockY - 1) downTo world.minHeight) {
            val block = world.getBlockAt(location.blockX, y, location.blockZ)

            if (block.type == Material.AIR || block.type == Material.CAVE_AIR || block.type == Material.VOID_AIR) {
                continue
            }

            if (block.type == Material.BEDROCK) return

            removePower(10)
            val drops = block.getDrops()
            block.setType(Material.AIR, false)

            val dropLocation = location.clone().add(0.5, 1.0, 0.5)
            for (item in drops) {
                world.dropItemNaturally(dropLocation, item)
            }
            return
        }
    }
}
