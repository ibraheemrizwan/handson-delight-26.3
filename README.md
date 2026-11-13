<p align="center"><img src="https://raw.githubusercontent.com/Pocky-l/handson-delight/df40913b749a07bb1e46be2161a7a54654cfa517/docs/icon.png" width="128" alt="Hands-On Delight logo"></p>

<h1 align="center">Hands-On Delight</h1>
<p align="center"><strong>Stir. Chop. Fry. Grill.</strong><br>Minecraft 26.3 · Fabric · Unofficial port</p>
<p align="center"><a href="https://github.com/ibraheemrizwan/handson-delight-26.3/releases/latest"><strong>Download</strong></a> · <a href="#installation">Installation</a> · <a href="#gallery">Gallery</a> · <a href="https://github.com/ibraheemrizwan/handson-delight-26.3/issues">Report a port issue</a> · <a href="https://github.com/Pocky-l/handson-delight">Original project</a></p>

<p align="center"><img alt="Minecraft 26.3" src="https://img.shields.io/badge/Minecraft-26.3-62B47A"> <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-C6BBA5"> <img alt="Java 25" src="https://img.shields.io/badge/Java-25-blue"> <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/License-MIT-blue"></a></p>

Cook with your hands in Farmer’s Delight. Its cooking pot, cutting board, skillet and stove become interactive stations inside the world.

<p align="center"><img src="https://raw.githubusercontent.com/Pocky-l/handson-delight/df40913b749a07bb1e46be2161a7a54654cfa517/docs/showcase/stir.gif" alt="Original Hands-On Delight showcase by Pocky" width="860"><br><sub>Original-mod showcase and artwork by Pocky. Screenshots of this 26.3 port appear in the gallery below.</sub></p>

## Features

- Stir soup, serve dishes and use an in-world recipe book.
- Chop ingredients and assemble meals on the cutting board.
- Toss food in the skillet and turn food on the stove.
- Nearby players see the cooking through synchronized station state.

## Installation

| Dependency | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | [0.161.0+26.3 or newer](https://modrinth.com/mod/fabric-api/versions?g=26.3) |
| Diegesis | [26.3 port](https://github.com/ibraheemrizwan/diegesis-26.3/releases/latest) |
| Farmer’s Delight Refabricated | [26.3-3.6.30](https://modrinth.com/mod/farmers-delight-refabricated/version/dRScyUeE) |

Download the **1.1.0+26.3.port.1** mod JAR from [Releases](https://github.com/ibraheemrizwan/handson-delight-26.3/releases/latest), then place it in your `mods` folder with its dependencies. Install the matching versions on the client and server for synchronized features. Replace older copies of the same mod.

This repository provides a Fabric build. Original mod and artwork: **Pocky**. Port maintenance: **ibraheemrizwan**.

## Gallery

These screenshots were captured in Minecraft 26.3 during the port’s automated client tests. They show the test scenes and the default renderer.

![Hands-On Delight during the Minecraft 26.3 port tests](docs/gallery/0001_handson-after-network-chop.png)

![Hands-On Delight during the Minecraft 26.3 port tests](docs/gallery/0003_handson-pot-after-resource-reload.png)

## Validation and compatibility

The recorded release passed **60 server GameTests**. The full four-port suite passed **136 server GameTests**, 26 additional kernel checks, dedicated-server integration, and client checks covering resource reload and saved-world reload. See [validation.json](validation.json) for release hashes and [TESTING.md](TESTING.md) for scope.

JEI 31.17.0.109 and REI 26.3.824 were tested in separate suite runs. EMI integration is excluded because a Fabric 26.3 release was unavailable when this port was built.

## Build from source

Install **JDK 25**, then run:

```sh
./gradlew build
```

On Windows, run `gradlew.bat build`. The mod JAR is written to `build/libs/`. The Gradle wrapper downloads its build tools and dependencies.

The port uses Loom 1.17.21, Gradle 9.6.0, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3. Dependencies used only for compilation are not bundled into the mod JAR.

## Credits and license

Original [source, design and artwork](https://github.com/Pocky-l/handson-delight) by **Pocky**, distributed under the [MIT license](LICENSE). This independent port preserves the original credits and is not an official upstream release. See [NOTICE.md](NOTICE.md).

## More Minecraft 26.3 ports

[Exposure](https://github.com/ibraheemrizwan/exposure-26.3) · [Catalog](https://github.com/ibraheemrizwan/exposure-catalog-26.3) · [Polaroid](https://github.com/ibraheemrizwan/exposure-polaroid-26.3) · [Envelope](https://github.com/ibraheemrizwan/envelope-26.3) · [Hoarfrost](https://github.com/ibraheemrizwan/hoarfrost-26.3) · [Petrichor](https://github.com/ibraheemrizwan/petrichor-26.3) · [Diegesis](https://github.com/ibraheemrizwan/diegesis-26.3) · [Hands-On Delight](https://github.com/ibraheemrizwan/handson-delight-26.3)
