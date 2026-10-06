package com.coderjoe.atlas.testing

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry

/**
 * Fixtures for laying out blocks by hand. A test drives a block's update by calling its internal
 * hook - `powerUpdate()`, `fluidUpdate()` - directly.
 *
 * Blocks themselves are placed with [com.coderjoe.atlas.block.BlockRegistry.track], which gives
 * them their context without starting their tick tasks.
 */
object Blocks {
    /** Tracks this block in [registry] under its own id and hands it back, for tests that need one in place. */
    fun <T : AtlasBlock> T.placedIn(registry: BlockRegistry): T {
        registry.track(this, baseBlockId)
        return this
    }
}
