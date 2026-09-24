package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Gauge
import com.coderjoe.atlas.block.Inspection
import com.coderjoe.atlas.block.capability.PowerConsumer
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import org.bukkit.Location
import org.bukkit.block.BlockFace

abstract class PowerBlock(
    location: Location,
    val maxStorage: Int,
    var currentPower: Int = 0,
) : AtlasBlock(location) {
    protected open val canReceivePower: Boolean = true

    /**
     * Whether this block exists to hold power rather than to produce or spend it.
     *
     * Storage sits on a run as both a source and a sink, so without knowing which blocks are
     * storage the network would move charge from one battery to another and straight back again.
     */
    open val isStorage: Boolean = false

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
     * Whether power can be drawn from this block right now.
     *
     * Usually that just means it is holding some, but a cable holds nothing of its own and
     * answers for the run it belongs to instead. Anything asking "is there power here?" rather
     * than moving it should ask this.
     */
    open fun canSupplyPower(): Boolean = hasPower()

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
    open fun removePowerToward(
        face: BlockFace,
        amount: Int,
    ): Int {
        if (!canOutputToward(face)) return 0
        return removePower(amount)
    }

    /**
     * Whether this block accepts power pushed in through [face], where [face] points from this
     * block toward the pusher. Blocks with a designated input face override this; by default a
     * block takes power from any side.
     */
    open fun canAcceptFrom(face: BlockFace): Boolean = true

    /**
     * Whether power could ever move through [face], where [face] points from this block toward
     * the neighbour. This asks about the port, not the moment: a battery that happens to be full
     * still connects, because it will take power again as soon as it drains.
     *
     * A cable uses this to decide which faces to grow an arm on, so the wiring a player sees
     * matches where power can actually flow - a panel that only outputs from its base draws no
     * arm on its sides.
     */
    open fun canConnectToward(face: BlockFace): Boolean = canOutputToward(face) || (canReceivePower && canAcceptFrom(face))

    /**
     * Face-aware counterpart to [addPower]. [face] points from this block toward the pusher, and
     * nothing is accepted when [canAcceptFrom] rejects that face.
     */
    fun addPowerFrom(
        face: BlockFace,
        amount: Int,
    ): Int {
        if (!canAcceptFrom(face)) return 0
        return addPower(amount)
    }

    /**
     * Hands up to [amount] of stored power to the neighbour on [face], honouring this block's own
     * output rules and the receiver's input rules.
     *
     * Power is debited before it is offered, so a refused push can never leave the same unit
     * counted in two blocks at once; whatever the receiver declines is credited straight back.
     * Returns how much the neighbour actually took.
     */
    protected fun pushPowerToward(
        face: BlockFace,
        amount: Int = currentPower,
    ): Int {
        if (amount <= 0) return 0
        val neighbor = BlockRegistry.active?.getAdjacentBlock(location, face) ?: return 0

        if (neighbor is PowerBlock) {
            if (!neighbor.canAcceptPower()) return 0
            val offered = removePowerToward(face, amount)
            if (offered <= 0) return 0
            val accepted = neighbor.addPowerFrom(face.oppositeFace, offered)
            if (accepted < offered) addPower(offered - accepted)
            return accepted
        }

        // A neighbour that spends power without being a power block - the fluid pump is the one -
        // is pushed to like anything else rather than left to reach back for what it needs.
        val consumer = neighbor as? PowerConsumer ?: return 0
        if (!consumer.drawsPowerFrom(face.oppositeFace) || !consumer.wantsPower()) return 0

        val offered = removePowerToward(face, amount)
        if (offered <= 0) return 0
        val accepted = consumer.acceptPower(face.oppositeFace, offered)
        if (accepted < offered) addPower(offered - accepted)
        return accepted
    }

    protected fun pullPowerFromNeighbors() {
        if (!canAcceptPower()) return
        val registry = BlockRegistry.active ?: return
        for (face in ADJACENT_FACES) {
            if (!canAcceptPower()) break
            val neighbor = registry.adjacentOf<PowerBlock>(location, face) ?: continue
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

    override fun writeSaveData(data: MutableMap<String, Any>) {
        data["currentPower"] = currentPower
    }

    override fun readSaveData(data: Map<String, Any?>) {
        currentPower = (data["currentPower"] as? Number)?.toInt() ?: 0
    }

    override fun inspect(): Inspection {
        return Inspection(
            gauges =
                listOf(
                    Gauge(
                        "Power",
                        currentPower,
                        maxStorage,
                    ),
                ),
        )
    }

    override fun blockUpdate() {
        powerUpdate()
    }
}
