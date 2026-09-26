package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.deposit.DepositMap
import org.bukkit.plugin.java.JavaPlugin

/**
 * What a block can reach once it is registered: the plugin that schedules its tasks, the registry
 * it finds its neighbours in, and the map of the ore in the ground beneath it. Handed over by [BlockRegistry] instead of looked up
 * globally, so a block needs nothing set up ahead of it beyond the registry it lives in.
 */
class BlockContext(
    val plugin: JavaPlugin,
    val registry: BlockRegistry,
    val deposits: DepositMap,
)
