package com.coderjoe.atlas.core

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry

interface BlockPersister<T : AtlasBlock> {
    fun save(registry: BlockRegistry<T>)

    fun load(registry: BlockRegistry<T>)
}
