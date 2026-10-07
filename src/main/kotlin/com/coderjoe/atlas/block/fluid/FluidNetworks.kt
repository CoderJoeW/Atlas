package com.coderjoe.atlas.block.fluid

/**
 * Finds the network a given pipe belongs to.
 *
 * Each run of touching pipe is discovered by one flood fill and then shared by all of its pipes
 * until a pipe, or a block against one, is placed or broken - see [FluidGrid]. Asking again in
 * between costs a map lookup rather than another walk of the run.
 *
 * Touching pipe is not automatically the same run. A run takes its identity from the source
 * feeding it, and a lava run that meets a water run stays a separate network on either side of
 * where they meet - otherwise butting two lines together would silently blend them, and which
 * fluid won would come down to which provider the scan happened to reach first. Which pipe
 * carries which fluid is re-read whenever a block touching the run changes what it holds.
 */
object FluidNetworks {
    fun networkFor(start: FluidPipe): FluidNetwork = start.grid.networkFor(start)
}
