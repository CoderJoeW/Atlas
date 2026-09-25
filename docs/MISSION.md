# Atlas: Mission & Guiding Principles

This document exists to settle arguments. When a design, feature, or trade-off is unclear, check it against the
principles below, in order.

## Mission

**Bring large-scale factory building and working, visible machines to Minecraft, with no client mods.**

A player with a stock Minecraft client joins the server, accepts the resource pack, and plays industrial Minecraft.
There is nothing to install and no modpack to manage.

## Why Atlas exists

Atlas grew out of a love of factory-building games and automation mods. Those mods prove that Minecraft players want
working machines and automation, but they need a modded client. That locks out anyone who won't or can't install a
modpack. Atlas delivers that gameplay entirely on the server.

## Principles, in priority order

When two principles conflict, the higher one wins.

### 1. No client mods, ever

A vanilla Java client plus the server's resource pack is the whole contract. Nothing may require a client-side mod,
launcher, or modpack. This is a hard line, not a preference.

- **This means:** every feature is built from what the server can do: blocks, display entities, the
  resource pack, dialogs, and vanilla packets.
- **This rules out:** any feature that only works properly with a client mod installed, even as an "optional
  enhancement".

### 2. Layer on top of vanilla, don't replace it

Vanilla survival stays intact. Atlas adds an industrial track built from vanilla materials. Automation is the reward
for vanilla progress, never a shortcut past it.

- **This means:** a machine's recipe reflects the tier it automates. A diamond mine costs diamond blocks and a
  netherite mine costs netherite. You must have done the vanilla work before you can automate it.
- **This rules out:** early-game machines that skip exploration, mining, or the Nether.
- **Test:** "Could a player get this without ever engaging with the vanilla game it automates?" If yes, it's too
  cheap or too early.

### 3. Discoverable in game

A player should be able to learn Atlas without a wiki. Recipes unlock naturally, tooltips and dialogs explain, and a
machine's state (idle, working, starved of power, full) can be read by looking at it.

- **This means:** every machine shows its state visually, and every mechanic has an in-game explanation.
- **This rules out:** hidden mechanics, magic numbers the player can't observe, and states that look identical.

### 4. Every machine is a joy to watch, and every factory is a joy to plan

Each machine should be tactile and satisfying on its own, with visible
motion, clear feedback, and a sense of work being done. The factory as a whole should reward planning: ratios,
throughput, logistics, and growth.

- **This means:** a new machine is judged on both how it looks working and how it fits into a production chain.
- **This rules out:** machines that are pure stat blocks with no visual life, and machines that look great but have
  no place in a chain.

### 5. Vanilla-plus look

Machines should look like something that could belong in Minecraft: readable at a glance, consistent with each other,
and not jarring next to vanilla blocks.

## Scope

**In scope**

- Industry and logistics: power, fluids, item transport, processing, and resource generation
- Trains, vehicles, and moving contraptions
- Player gear and tools unlocked by the factory

**Out of scope**

- Magic, and combat content that isn't tech gear
- Economy or shop plugin integration
- Multiplayer protection (claims, ownership, anti-grief), which is deferred for now and assumes trusted players.

## Configuration

Ship one well-balanced experience with opinionated defaults. Add server-owner configuration only when real servers ask
for it, and not speculatively.

## Goals: what success looks like

1. **Players choose Atlas over modpacks.** People who would have installed a modpack for industrial gameplay play on
   an Atlas server instead.
2. **Easy drop-in.** A Paper server owner installs it and it works, with no manual resource-pack wrangling or fragile
   setup.
3. **Our community loves it.** Our own players build big factories and keep coming back.
4. **A complete tech tree.** A coherent progression from the first generator to an endgame, where every tier is earned
   through vanilla progress.

## Using this document

When evaluating a feature, ask in order:

1. Does it work on an unmodded client?
2. Does it respect vanilla progression?
3. What does it cost the server at scale?
4. Can a player figure it out without leaving the game?
5. Is it satisfying to watch, and does it earn a place in a production chain?
6. Does it look like it belongs in Minecraft?

A "no" at step 1 ends the discussion. A "no" further down means redesign, not necessarily rejection.
