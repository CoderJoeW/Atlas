package com.coderjoe.atlas.core

import org.bukkit.Location
import org.bukkit.block.BlockFace

enum class PlacementType {
    SIMPLE,
    DIRECTIONAL,
    DIRECTIONAL_OPPOSITE,
}

data class BlockDescriptor(
    val baseBlockId: String,
    val displayName: String,
    val description: String,
    val placementType: PlacementType,
    val directionalVariants: Map<BlockFace, String>,
    val allRegistrableIds: List<String>,
    val constructor: (Location, BlockFace) -> AtlasBlock,
)
