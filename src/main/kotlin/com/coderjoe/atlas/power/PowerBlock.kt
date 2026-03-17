package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import org.bukkit.Location

abstract class PowerBlock(
    location: Location,
    val maxStorage: Int,
    var currentPower: Int = 0,
) : AtlasBlock(location) {
    protected open val canReceivePower: Boolean = true

    fun hasPower(): Boolean = currentPower > 0

    fun canAcceptPower(): Boolean = canReceivePower && currentPower < maxStorage

    fun addPower(amount: Int): Int {
        val spaceAvailable = maxStorage - currentPower
        val toAdd = minOf(amount, spaceAvailable)
        currentPower += toAdd
        return toAdd
    }

    fun removePower(amount: Int): Int {
        val toRemove = minOf(amount, currentPower)
        currentPower -= toRemove
        return toRemove
    }

    protected fun pullPowerFromNeighbors() {
        if (!canAcceptPower()) return
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

    protected abstract fun powerUpdate()

    override fun blockUpdate() {
        powerUpdate()
    }

    override fun getRegistry(): BlockRegistry<*> {
        return PowerBlockRegistry.instance ?: throw IllegalStateException("PowerBlockRegistry not initialized")
    }
}
