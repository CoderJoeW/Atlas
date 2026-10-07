package com.coderjoe.atlas

import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.craftengine.CraftEngineIntegration
import com.coderjoe.atlas.data.BlockPersistence
import com.coderjoe.atlas.data.ChunkOreSurvey
import com.coderjoe.atlas.hologram.DepositReadout
import com.coderjoe.atlas.hologram.HologramInspector
import com.coderjoe.atlas.item.AtlasGoggles
import com.coderjoe.atlas.item.GuideBook
import com.coderjoe.atlas.listener.AtlasBlockListener
import com.coderjoe.atlas.listener.ChunkSurveyListener
import com.coderjoe.atlas.listener.GuideBookListener
import com.coderjoe.atlas.listener.PlayerJoinListener
import com.coderjoe.atlas.listener.WorldLoadListener
import com.coderjoe.atlas.util.AtlasConfig
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class Atlas : JavaPlugin() {
    private lateinit var craftEngineIntegration: CraftEngineIntegration
    private lateinit var registry: BlockRegistry
    private lateinit var persistence: BlockPersistence
    private var hologramInspector: HologramInspector? = null
    private var depositReadout: DepositReadout? = null
    private var oreSurvey: ChunkOreSurvey? = null
    private var autoSaveTask: BukkitTask? = null

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        AtlasConfig.load(this)

        craftEngineIntegration = CraftEngineIntegration(this)
        craftEngineIntegration.initialize()

        server.pluginManager.registerEvents(PlayerJoinListener(), this)

        val survey = ChunkOreSurvey(this)
        oreSurvey = survey
        server.pluginManager.registerEvents(ChunkSurveyListener(survey), this)
        server.worlds.flatMap { it.loadedChunks.asList() }.forEach(survey::survey)

        registry = BlockRegistry(this, survey)
        val catalog = AtlasBlockTypes.catalog
        persistence = BlockPersistence(this, catalog).also { it.load(registry) }
        logger.atlasInfo("Block registry initialized with ${catalog.blockIds.size} block types")

        server.pluginManager.registerEvents(WorldLoadListener(persistence, registry), this)
        server.pluginManager.registerEvents(AtlasBlockListener(this, registry, catalog), this)
        hologramInspector =
            HologramInspector(this, registry, catalog) { AtlasGoggles.isWearing(it, this) }
                .also { it.start() }
        depositReadout =
            DepositReadout(this, survey::oreIn) { AtlasGoggles.isWearing(it, this) }
                .also { it.start() }

        val guideBookListener = GuideBookListener(this)
        server.pluginManager.registerEvents(guideBookListener, this)
        server.addRecipe(GuideBook.createRecipe(this))
        server.addRecipe(AtlasGoggles.createRecipe(this))

        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTask =
            server.scheduler.runTaskTimer(
                this,
                Runnable { persistence.save(registry) },
                6000L, 6000L,
            )

        logger.atlasInfo("Atlas plugin enabled!")
    }

    override fun onDisable() {
        autoSaveTask?.cancel()

        if (::persistence.isInitialized) persistence.save(registry)

        hologramInspector?.stop()
        depositReadout?.stop()
        oreSurvey?.stop()

        if (::registry.isInitialized) registry.stopAll()

        logger.atlasInfo("Atlas plugin has been disabled!")
    }
}
