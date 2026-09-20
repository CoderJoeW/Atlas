package com.coderjoe.atlas.block.capability

import org.bukkit.Location

interface ItemInlet {
    fun itemDropLocation(): Location
}