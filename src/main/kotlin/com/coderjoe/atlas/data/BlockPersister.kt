package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockRegistry

interface BlockPersister {
    fun save(registry: BlockRegistry)

    fun load(registry: BlockRegistry)
}
