package com.coderjoe.atlas.block.deposit

import org.bukkit.Location

/** Where the world's ore lies: how rich the chunk holding a location is in each [Ore]. */
fun interface DepositMap {
    /** The purity of [ore] in the chunk holding [location], or null while that chunk is still being surveyed. */
    fun purityAt(
        location: Location,
        ore: Ore,
    ): Purity?

    companion object {
        /** Every chunk a Normal deposit of everything, for worlds with no survey behind them. */
        val UNIFORM = DepositMap { _, _ -> Purity.NORMAL }
    }
}
