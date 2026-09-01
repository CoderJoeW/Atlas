package com.coderjoe.atlas.power

import org.bukkit.block.BlockFace

/**
 * The pair of faces a splitter branches to, and that a merger collects from alongside its back.
 *
 * For a horizontal facing these are the two horizontal faces perpendicular to it. A vertical
 * facing has four perpendicular faces and no natural pair, so north and south are used.
 */
fun branchFaces(facing: BlockFace): List<BlockFace> =
    when (facing) {
        BlockFace.NORTH, BlockFace.SOUTH -> listOf(BlockFace.EAST, BlockFace.WEST)
        BlockFace.EAST, BlockFace.WEST -> listOf(BlockFace.NORTH, BlockFace.SOUTH)
        else -> listOf(BlockFace.NORTH, BlockFace.SOUTH)
    }
