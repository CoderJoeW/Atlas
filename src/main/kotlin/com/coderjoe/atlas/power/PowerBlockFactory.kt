package com.coderjoe.atlas.power

import org.bukkit.Location

/**
 * Factory for creating PowerBlock instances based on block IDs
 */
object PowerBlockFactory {
    private val blockConstructors = mutableMapOf<String, (Location) -> PowerBlock>()

    /**
     * Registers a PowerBlock type with its block ID
     * @param blockId The Nexo block ID (e.g., "small_solar_panel")
     * @param constructor A function that creates the PowerBlock instance given a location
     */
    fun register(blockId: String, constructor: (Location) -> PowerBlock) {
        blockConstructors[blockId] = constructor
        println("PowerBlockFactory: Registered block ID '$blockId'")
    }

    /**
     * Creates a PowerBlock instance for the given block ID and location
     * @return PowerBlock instance, or null if the block ID is not registered
     */
    fun createPowerBlock(blockId: String, location: Location): PowerBlock? {
        return blockConstructors[blockId]?.invoke(location)
    }

    /**
     * Checks if a block ID is registered as a power block
     */
    fun isRegistered(blockId: String): Boolean {
        return blockConstructors.containsKey(blockId)
    }

    /**
     * Gets all registered block IDs
     */
    fun getRegisteredBlockIds(): Set<String> {
        return blockConstructors.keys
    }
}
