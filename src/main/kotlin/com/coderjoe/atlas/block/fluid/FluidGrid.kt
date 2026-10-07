package com.coderjoe.atlas.block.fluid

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.BlockRun
import com.coderjoe.atlas.block.RunCache
import com.coderjoe.atlas.block.capability.FluidType

/**
 * Every run of touching pipe in one registry, each kept until the shape of the run changes.
 *
 * The ticker runs [tick] once per interval, and each [FluidNetwork] transfers exactly once in it
 * however many pipes it is made of.
 */
class FluidGrid(registry: BlockRegistry) {
    companion object {
        const val TICK_INTERVAL = 20L

        fun of(registry: BlockRegistry): FluidGrid = registry.system(FluidGrid::class.java, ::FluidGrid)
    }

    private val runs = RunCache(FluidPipe::class.java, ::PipeRun)

    init {
        registry.ticker.addSystem("fluid networks", TICK_INTERVAL, ::tick)
    }

    fun networkFor(pipe: FluidPipe): FluidNetwork = runs.runFor(pipe).networkFor(pipe)

    fun networks(): List<FluidNetwork> = runs.runs().flatMap { it.networks() }

    /**
     * Ticks every network once. Labels are re-read from the sources first, so even a source
     * whose supply changed without anyone saying so is caught within one interval.
     */
    fun tick() {
        for (run in runs.runs()) {
            run.labelsChanged()
            for (network in run.networks()) network.tick()
        }
    }

    /** A fluid block's contents changed, which can move where its runs' fluids meet. */
    internal fun fluidChangedAt(block: FluidBlock) {
        for (face in AtlasBlock.ADJACENT_FACES) {
            val pipe = block.neighbor(face) as? FluidPipe ?: continue
            runs.cached(pipe)?.labelsChanged()
        }
    }

    internal fun placed(pipe: FluidPipe) = runs.placed(pipe)

    internal fun removed(pipe: FluidPipe) = runs.removed(pipe)

    internal fun neighborChanged(pipe: FluidPipe) = runs.invalidate(pipe)

    /**
     * A run of touching pipe, split into one [FluidNetwork] per fluid - see [FluidNetworks].
     *
     * The split depends on which sources hold what, so it is redone lazily after a source changes,
     * and only rebuilt when that actually moves a boundary.
     */
    private class PipeRun(private val run: BlockRun<FluidPipe>) {
        private val indexOf = HashMap<FluidPipe, Int>().apply { run.members.forEachIndexed { i, pipe -> put(pipe, i) } }
        private val edgesOf: List<List<BlockRun.Edge>> = run.members.indices.map { i -> run.edges.filter { it.memberIndex == i } }
        private var stale = true
        private var seeds: List<FluidType?> = emptyList()
        private var labels: List<FluidType> = emptyList()
        private var networkOf: List<FluidNetwork> = emptyList()

        fun labelsChanged() {
            stale = true
        }

        fun networkFor(pipe: FluidPipe): FluidNetwork {
            refresh()
            return networkOf[indexOf.getValue(pipe)]
        }

        fun networks(): List<FluidNetwork> {
            refresh()
            return networkOf.distinct()
        }

        private fun refresh() {
            if (!stale) return
            stale = false
            val fresh = seeds()
            if (fresh == seeds && networkOf.isNotEmpty()) return
            seeds = fresh

            val spread = spread(fresh)
            if (spread == labels && networkOf.isNotEmpty()) return
            labels = spread
            networkOf = partition(spread)
        }

        /**
         * The fluid each pipe is fed directly, from a source touching it. A pipe touching two
         * sources at once has to pick one; the lower ordinal is an arbitrary rule, but a stable
         * one, so the run does not flicker between them.
         */
        private fun seeds(): List<FluidType?> =
            edgesOf.map { edges ->
                edges
                    .mapNotNull { edge ->
                        val source = edge.block as? FluidBlock ?: return@mapNotNull null
                        if (source.canProvideFluid(edge.faceTowardRun) && source.hasFluid()) source.storedFluid else null
                    }.minByOrNull { it.ordinal }
            }

        /**
         * Labels each pipe with the fluid of the nearest source feeding it, by a flood fill outward
         * from every source at once. Where a lava line and a water line meet, each pipe belongs
         * to whichever source it sits closer to and the boundary falls between them. A pipe no
         * source reaches reads as carrying nothing, which keeps an unfed run behaving as the
         * single network it looks like.
         */
        private fun spread(seeds: List<FluidType?>): List<FluidType> {
            val labelled = seeds.toMutableList()
            val queue = ArrayDeque(labelled.indices.filter { labelled[it] != null })
            while (queue.isNotEmpty()) {
                val pipe = queue.removeFirst()
                for (neighbor in run.links[pipe]) {
                    if (labelled[neighbor] != null) continue
                    labelled[neighbor] = labelled[pipe]
                    queue.add(neighbor)
                }
            }
            return labelled.map { it ?: FluidType.NONE }
        }

        /** Splits the run wherever two touching pipes carry different fluids. */
        private fun partition(labels: List<FluidType>): List<FluidNetwork> {
            val networks = arrayOfNulls<FluidNetwork>(labels.size)
            for (start in labels.indices) {
                if (networks[start] != null) continue
                val members = mutableListOf(start)
                val included = hashSetOf(start)
                var next = 0
                while (next < members.size) {
                    for (neighbor in run.links[members[next++]]) {
                        if (labels[neighbor] == labels[start] && included.add(neighbor)) members += neighbor
                    }
                }
                val network = FluidNetwork(members.map { run.members[it] }, members.flatMap { edgesOf[it] })
                members.forEach { networks[it] = network }
            }
            return networks.map { checkNotNull(it) }
        }
    }
}
