package com.coderjoe.atlas.util

import org.bukkit.block.BlockFace

fun BlockFace.displayName(): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
}