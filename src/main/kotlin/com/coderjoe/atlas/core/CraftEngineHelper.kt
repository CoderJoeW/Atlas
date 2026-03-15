package com.coderjoe.atlas.core

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.block.properties.Property
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

object CraftEngineHelper {
    private val CE_FACING_TO_BLOCK_FACE = mapOf(
        "north" to BlockFace.NORTH,
        "south" to BlockFace.SOUTH,
        "east" to BlockFace.EAST,
        "west" to BlockFace.WEST,
        "up" to BlockFace.UP,
        "down" to BlockFace.DOWN,
    )

    fun getBlockId(block: Block): String? {
        return try {
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return null
            state.owner().value().id().toString()
        } catch (_: Throwable) {
            null
        }
    }

    fun getFacing(block: Block): BlockFace {
        return try {
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return BlockFace.NORTH
            val customBlock = state.owner().value()
            val facingProp = customBlock.getProperty("facing") ?: return BlockFace.NORTH
            val facingValue = state.get(facingProp).toString()
            CE_FACING_TO_BLOCK_FACE[facingValue] ?: BlockFace.NORTH
        } catch (_: Throwable) {
            BlockFace.NORTH
        }
    }

    fun setFacing(location: Location, facing: BlockFace): Boolean {
        return try {
            val block = location.block
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return false
            val customBlock = state.owner().value()
            val facingProp = customBlock.getProperty("facing") ?: return false
            val currentFacing = state.get(facingProp).toString()
            val targetFacing = facing.name.lowercase()
            if (currentFacing != targetFacing) {
                @Suppress("UNCHECKED_CAST")
                val typedProp = facingProp as Property<Comparable<Comparable<*>>>
                val possibleValues = typedProp.possibleValues()
                val targetValue = possibleValues.find {
                    typedProp.valueName(it) == targetFacing
                } ?: return false
                val newState = state.with(typedProp, targetValue)
                CraftEngineBlocks.place(location, newState, false)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun setBooleanProperty(location: Location, propertyName: String, value: Boolean) {
        try {
            val block = location.block
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return
            val customBlock = state.owner().value()
            val prop = customBlock.getProperty(propertyName) ?: return
            val currentValue = state.get(prop) as? Boolean ?: return
            if (currentValue != value) {
                @Suppress("UNCHECKED_CAST")
                val typedProp = prop as Property<Boolean>
                val newState = state.with(typedProp, value)
                CraftEngineBlocks.place(location, newState, false)
            }
        } catch (_: Throwable) {
            // CraftEngine not available
        }
    }

    fun setStringProperty(location: Location, propertyName: String, value: String) {
        try {
            val block = location.block
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return
            val customBlock = state.owner().value()
            val prop = customBlock.getProperty(propertyName) ?: return
            @Suppress("UNCHECKED_CAST")
            val typedProp = prop as Property<Comparable<Comparable<*>>>
            val possibleValues = typedProp.possibleValues()
            val targetValue = possibleValues.find {
                typedProp.valueName(it) == value
            } ?: return
            val currentValue = state.get(typedProp)
            if (currentValue != targetValue) {
                val newState = state.with(typedProp, targetValue)
                CraftEngineBlocks.place(location, newState, false)
            }
        } catch (_: Throwable) {
            // CraftEngine not available
        }
    }

    fun setIntProperty(location: Location, propertyName: String, value: Int) {
        try {
            val block = location.block
            val state = CraftEngineBlocks.getCustomBlockState(block) ?: return
            val customBlock = state.owner().value()
            val prop = customBlock.getProperty(propertyName) ?: return
            val currentValue = state.get(prop) as? Int ?: return
            if (currentValue != value) {
                @Suppress("UNCHECKED_CAST")
                val typedProp = prop as Property<Int>
                val newState = state.with(typedProp, value)
                CraftEngineBlocks.place(location, newState, false)
            }
        } catch (_: Throwable) {
            // CraftEngine not available
        }
    }
}
