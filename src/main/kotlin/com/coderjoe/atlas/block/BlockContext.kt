package com.coderjoe.atlas.block

import org.bukkit.plugin.java.JavaPlugin

/**
 * What a block can reach once it is registered: the plugin that schedules its tasks, and the
 * registry it finds its neighbours in. Handed over by [BlockRegistry] instead of looked up
 * globally, so a block needs nothing set up ahead of it beyond the registry it lives in.
 */
class BlockContext(val plugin: JavaPlugin, val registry: BlockRegistry)
