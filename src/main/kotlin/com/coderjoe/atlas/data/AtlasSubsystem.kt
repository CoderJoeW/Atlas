package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockFactory
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.plugin.java.JavaPlugin

class AtlasSubsystem(
    private val name: String,
    val registry: BlockRegistry,
    private val factory: BlockFactory,
    val descriptors: Map<String, BlockDescriptor>,
    private val persistence: BlockPersister,
    private val plugin: JavaPlugin,
) {
    fun init() {
        factory.registerFromDescriptors(descriptors.values)
        persistence.load(registry)
        val label = name.replaceFirstChar { it.uppercase() }
        val blockTypeCount = factory.getRegisteredBlockIds().size
        plugin.logger.atlasInfo("$label system initialized with $blockTypeCount block types")
    }

    fun save() = persistence.save(registry)

    fun stop() = registry.stopAll()
}
