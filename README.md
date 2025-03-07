![](fancynpcs_title.png)

#                  

![Latest Version](https://img.shields.io/github/v/release/FancyMcPlugins/FancyNpcs?style=flat-square)
[![Generic badge](https://img.shields.io/badge/folia-supported-green.svg)](https://shields.io/)
[![Discord](https://img.shields.io/discord/899740810956910683?color=7289da&logo=Discord&label=Discord&style=flat-square)](https://discord.gg/ZUgYCEJUEx)
![GitHub Downloads](https://img.shields.io/github/downloads/FancyMcPlugins/FancyNpcs/total?logo=GitHub&style=flat-square)
[![SpigotMC Downloads](https://badges.spiget.org/resources/downloads/spigotmc-orange-107306.svg)](https://www.spigotmc.org/resources/npc-plugin-1-19-4.107306/)
[![Downloads](https://img.shields.io/modrinth/dt/fancynpcs?color=00AF5C&label=modrinth&style=flat&logo=modrinth)](https://modrinth.com/plugin/fancynpcs/versions)

Simple, lightweight and fast NPC plugin using [packets](https://wiki.vg/Protocol)

**Only for minecraft server version 1.19.4 - 1.20.4**<br>
_Using [paper](https://papermc.io/downloads) is highly recommended_

## Get the plugin

### Stable versions

- https://hangar.papermc.io/Oliver/FancyNpcs
- https://modrinth.com/plugin/fancynpcs/
- https://github.com/FancyMcPlugins/FancyNpcs/releases
- https://www.spigotmc.org/resources/fancy-npcs.107306/

### Development builds

- https://fancyplugins.de/fancynpcs/download/
- Build from source

## Features

With FancyNpcs you can create NPCs that look like real players. You can edit a bunch of properties like the display name
or skin.

All properties:

- display name
- skin (username or url)
- entity type (cow, pig, blaze etc.)
- equipment (e.g. holding something in the hand)
- glowing (in all colors)
- and many more properties that are entity specific

You can also define some actions that will be executed when a player interacts with the npc:

- message: sends a simple message to the player
- player command: forces the player to execute a command
- server command: executes a command in the console

Explore more features on the official website: https://fancyplugins.de/

## Documentation

### Plugin

Commands: [commands.md](docs/commands.md)<br>
Multiple lines: [multiple-lines.md](docs/misc/multiple-lines.md)<br>

### Api

Getting started: [getting-started.md](docs/api/getting-started.md)<br>
Events: [events.md](docs/api/events.md)<br>

### Build from source

1. Clone this repo and run `gradlew shadowJar`
2. The jar file will be in `build/libs/FancyNpcs-<version>.jar`

## Examples

The following images are by https://explorerseden.eu/
![](exampleImages/niceron2.png)

![](exampleImages/niceron1.png)

![](exampleImages/niceron3.png)

The following image is by Beacon's Quest
![](exampleImages/dave1.png)

The following images are by oliver (main dev of this plugin)
![](exampleImages/oliver1.png)

![](exampleImages/oliver2.png)

![](exampleImages/oliver3.png)

