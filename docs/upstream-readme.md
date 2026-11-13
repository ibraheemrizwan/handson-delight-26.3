# Original project documentation

This archived README describes the original 1.21.1 release. Consult this repository’s main README for the 26.3 Fabric port.

<h1 align="center">Hands-On Delight</h1>

<p align="center">
  Cook in <a href="https://www.curseforge.com/minecraft/mc-mods/farmers-delight">Farmer's Delight</a> with your own hands:
  the cooking pot, the cutting board, the skillet and the stove open right in the world.
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/hands-on-delight"><img alt="CurseForge" src="https://img.shields.io/badge/CurseForge-Hands--On%20Delight-F16436"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A">
  <a href="https://neoforged.net"><img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-D7742F"></a>
  <a href="https://fabricmc.net"><img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-C6BBA5"></a>
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

<p align="center">
  <img src="docs/showcase/stir.gif" alt="Vegetables stirred by hand in the open cooking pot" width="640">
</p>

An addon for [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight), built on
[Diegesis](https://www.curseforge.com/minecraft/mc-mods/diegesis). It adds no blocks or items: Farmer's Delight's own blocks
open as stations, and its recipes, and those of its addons, work in them.

<table align="center">
  <tr>
    <td align="center"><img src="docs/showcase/chop.gif" width="260" alt="A cabbage chopped on the cutting board"><br>Chop on the board</td>
    <td align="center"><img src="docs/showcase/skillet.gif" width="260" alt="Beef patties tossed in the skillet"><br>Toss the skillet</td>
    <td align="center"><img src="docs/showcase/burger.gif" width="260" alt="A hamburger stacked on the cutting board"><br>Build a burger</td>
  </tr>
</table>

## Features

- **Cooking pot.** Food floats in the water and colours it. Take the spoon by its handle and stir: the whole pot cooks
  as one batch, sixteen servings in about 27 seconds. Walk away and it cooks on as Farmer's Delight cooks, a serving at
  a time.
- **Stars.** Wide, steady rounds fill up to three stars over the dish. Two stars add a serving for every four, three
  stars one for every two. A dish opens its second star after 8 servings and its third after 32.
- **Recipe book.** It hangs beside the station and puts what you can make now first. Click a dish and its ingredients
  go into the pot from your inventory; Shift + click puts in up to sixteen sets.
- **Cutting board.** Right-click it with food to lay the food down, with a knife to take the knife in hand. Chop on
  the dashed marks; each cut goes through sixteen items of the stack. Food is cut across its own length, a kelp roll in
  even slices and a pie in wedges, with one cut for each piece it gives, three at most.
- **Dishes on the board.** Farmer's Delight's food crafts move from the crafting table to the cutting board: lay what
  goes down first (bread, a bowl, a stick, kelp, wheat, a pie crust), put the rest on it, and the dish is made. Salads
  are tossed in their bowl and plated meals laid in it, sandwiches closed with their bread, skewers threaded from the
  tip, dough kneaded. Milk and honey are poured, eggs cracked. Machines such as Create's and
  AE2's autocrafters still craft them, and so do players without this mod on a server that has it.
- **Board recipe book.** Tabs show everything, the dishes only or the cuts only. Click a dish and it is laid from your
  inventory; Shift + click lays sixteen.
- **Skillet.** Food fries on the side that lies on the pan, then smokes and waits. Toss the pan by its handle to turn it.
  When both sides are done, click the food to take it. Up to sixteen fry together. Eggs go in cracked and fry
  sunny side up, with no toss.
- **Stove.** Up to six pieces grill on its top. Each one smokes when its lower side is done: click it to turn it over.
  Once both sides are done it waits on the grill until you take it. Eggs would run through the grill: fry them in a
  skillet.
- **A kitchen.** Rest the cursor at the edge of the screen beside another station and the camera moves over to it. A, D
  and the arrow keys do the same.
- **For everyone.** Players nearby see the soup simmer, the spoon go round and the food land on the board.

## Controls

| Action | How |
|---|---|
| Open a station | Right-click the pot, board, skillet or stove |
| Use the block as in Farmer's Delight | Sneak + right-click |
| Stir | Click the spoon's handle, move the mouse, click again to put it down |
| Serve | Click the soup with a bowl |
| Put a whole stack in | Shift + click it in the inventory |
| Cut | Left-click the food on the board: the knife comes from your inventory |
| Turn a piece on the stove | Click it |
| Take food back | Right-click it |
| Put the knife away | Right-click with it |
| Go to the next station | Rest the cursor at the screen's edge on its side, or press A, D or an arrow key |

## Compatibility

[![JEI](https://raw.githubusercontent.com/Pocky-l/handson-delight/main/docs/compat/jei.png)](https://www.curseforge.com/minecraft/mc-mods/jei) ✅ **[JEI](https://www.curseforge.com/minecraft/mc-mods/jei)** (optional) - shows the board's dishes

[![EMI](https://raw.githubusercontent.com/Pocky-l/handson-delight/main/docs/compat/emi.png)](https://www.curseforge.com/minecraft/mc-mods/emi) ✅ **[EMI](https://www.curseforge.com/minecraft/mc-mods/emi)** (optional) - the same

[![REI](https://raw.githubusercontent.com/Pocky-l/handson-delight/main/docs/compat/rei.png)](https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items) ✅ **[REI](https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items)** (optional) - the same

[![Sodium](https://raw.githubusercontent.com/Pocky-l/handson-delight/1.21.1/docs/compat/sodium.png)](https://modrinth.com/mod/sodium) ✅ **[Sodium](https://modrinth.com/mod/sodium)** and [![Iris](https://raw.githubusercontent.com/Pocky-l/handson-delight/1.21.1/docs/compat/iris.png)](https://modrinth.com/mod/iris) **[Iris](https://modrinth.com/mod/iris)** (optional) - the stations draw as usual

Hoppers, pipes and drops use the blocks' own inventories. A pot fed by automation cooks by itself, as in Farmer's Delight.

## Configuration

`config/handson_delight.properties`, read at startup. On a server it decides for every player, except `recipe_book`,
which each player sets in their own file.

| Key | Default | What it does |
|---|---|---|
| `cooking_pot`, `cutting_board`, `skillet`, `stove` | `true` | The station of that block. Off, the block works as in Farmer's Delight |
| `remove_crafting_recipes` | `true` | Farmer's Delight's food crafts are made on the board only |
| `stirring_required` | `true` | Food you put into the pot waits for the spoon while you are at it |
| `recipe_book` | `true` | The recipe book beside the stations |

## Installation

1. Minecraft 1.21.1 with [NeoForge](https://neoforged.net) or [Fabric](https://fabricmc.net).
2. [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight), on Fabric
   [Farmer's Delight Refabricated](https://www.curseforge.com/minecraft/mc-mods/farmers-delight-refabricated) and
   [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
3. [Diegesis](https://www.curseforge.com/minecraft/mc-mods/diegesis) and this mod in the `mods` folder, on the client and
   the server. A client without it keeps Farmer's Delight's own menus.

## Building

```sh
./gradlew build
```

The jars are written to `neoforge/build/libs/` and `fabric/build/libs/`.

## Credits

- Author: **Pocky**.
- The broth sound is a recording by Joseph Sardin, released as CC0 on [BigSoundBank](https://bigsoundbank.com).
  Farmer's Delight's own sounds and models are used from the installed mod, never copied.

<!-- more-mods:start -->
## More mods by Pocky

**🌦️ Weather & nature**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/hoarfrost-snow-blizzards"><img src="docs/more-mods/hoarfrost.png" width="48" alt="Hoarfrost: Snow & Blizzards"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/hoarfrost-snow-blizzards"><b>Hoarfrost: Snow & Blizzards</b></a> - seven kinds of snowfall, a living wind and soft snow on the land (<a href="https://github.com/Pocky-l/hoarfrost">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><img src="docs/more-mods/petrichor.png" width="48" alt="Petrichor: Rain & Storms"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/petrichor-rain-storms"><b>Petrichor: Rain & Storms</b></a> - realistic rain, puddles, drips and branching lightning (<a href="https://github.com/Pocky-l/petrichor">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><img src="docs/more-mods/rustling_leaves.png" width="48" alt="Rustling Leaves"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/rustling-leaves"><b>Rustling Leaves</b></a> - falling leaves and leaf piles you can wade through and rake, client-side (<a href="https://github.com/Pocky-l/rustling-leaves">source</a>)</td>
  </tr>
</table>

**🎨 Graphics & performance**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons"><img src="docs/more-mods/dhturbo.png" width="48" alt="Turbo for Distant Horizons"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/turbo-for-distant-horizons"><b>Turbo for Distant Horizons</b></a> - Distant Horizons terrain generated several times faster (<a href="https://github.com/Pocky-l/dhturbo">source</a>)</td>
  </tr>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/shaders/pocky-shaders"><img src="docs/more-mods/pocky_shaders.png" width="48" alt="Pocky Shaders"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/shaders/pocky-shaders"><b>Pocky Shaders</b></a> - a cosy, natural shader pack that shows the weather of these mods (<a href="https://github.com/Pocky-l/pocky-shaders">source</a>)</td>
  </tr>
</table>

**🔮 Magic**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/hextale"><img src="docs/more-mods/hextale.png" width="48" alt="HexTale"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/hextale"><b>HexTale</b></a> - hextech magic to research, distil and bind with runes, inspired by Arcane and Thaumcraft (<a href="https://github.com/Pocky-l/HexTale">source</a>)</td>
  </tr>
</table>

**⚡ Tech & energy**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-advancedgenerators"><img src="docs/more-mods/mekanismadvancedgenerators.png" width="48" alt="Mekanism Advanced Generators"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/mekanism-advancedgenerators"><b>Mekanism Advanced Generators</b></a> - nine tiers of solar panels, big solar generators, cubes and cables for Mekanism (<a href="https://github.com/Pocky-l/Mekanism-MoreSolarPanels">source</a>)</td>
  </tr>
</table>

**🛠️ Server tools**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/inventory-backups"><img src="docs/more-mods/inventorybackups.png" width="48" alt="Inventory Backups"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/inventory-backups"><b>Inventory Backups</b></a> - inventory and ender chest backups with one-click restore for admins (<a href="https://github.com/Pocky-l/inventory-backups">source</a>)</td>
  </tr>
</table>

**🧩 Libraries & addons**

<table>
  <tr>
    <td width="56"><a href="https://www.curseforge.com/minecraft/mc-mods/diegesis"><img src="docs/more-mods/diegesis.png" width="48" alt="Diegesis"></a></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/diegesis"><b>Diegesis</b></a> - a library for interfaces you work with right on the block, with no menu screen (<a href="https://github.com/Pocky-l/diegesis">source</a>)</td>
  </tr>
</table>

<!-- more-mods:end -->

## License

[MIT](LICENSE)
