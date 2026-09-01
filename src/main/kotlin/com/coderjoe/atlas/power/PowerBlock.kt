package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlock
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.CraftEngineHelper
import org.bukkit.Location
import org.bukkit.block.BlockFace

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

    /**
     * Whether this block hands power out through [face], where [face] points from this block
     * toward the consumer. Sources with a dedicated output port override this; by default a
     * block can be drained from any side.
     */
    open fun canOutputToward(face: BlockFace): Boolean = true

    /**
     * Face-aware counterpart to [removePower]. [face] points from this block toward the
     * consumer, and extraction yields nothing when [canOutputToward] rejects that face.
     */
    fun removePowerToward(
        face: BlockFace,
        amount: Int,
    ): Int {
        if (!canOutputToward(face)) return 0
        return removePower(amount)
    }

    protected fun pullPowerFromNeighbors() {
        if (!canAcceptPower()) return
        val registry = PowerBlockRegistry.instance ?: return
        for (face in ADJACENT_FACES) {
            if (!canAcceptPower()) break
            val neighbor = registry.getAdjacentBlock(location, face) ?: continue
            if (neighbor.hasPower()) {
                val pulled = neighbor.removePowerToward(face.oppositeFace, 1)
                if (pulled > 0) {
                    addPower(pulled)
                }
            }
        }
    }

    protected fun updatePoweredState() {
        CraftEngineHelper.setBooleanProperty(location, "powered", hasPower())
    }

    protected abstract fun powerUpdate()

    override fun blockUpdate() {
        powerUpdate()
    }

    override fun getRegistry(): BlockRegistry<*> {
        return PowerBlockRegistry.instance ?: throw IllegalStateException("PowerBlockRegistry not initialized")
    }
}
