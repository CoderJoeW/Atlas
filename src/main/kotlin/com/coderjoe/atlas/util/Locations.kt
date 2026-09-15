package com.coderjoe.atlas.util

import org.bukkit.Location

val Location.coordinates: String
    get() = "$blockX,$blockY,$blockZ"
