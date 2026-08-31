package com.coderjoe.atlas.core

interface BlockPersister<T : AtlasBlock> {
    fun save(registry: BlockRegistry<T>)

    fun load(registry: BlockRegistry<T>)
}
