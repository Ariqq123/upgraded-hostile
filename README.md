# UpgradedHostile

![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1+-green?style=for-the-badge&logo=minecraft&logoColor=white)
![Paper](https://img.shields.io/badge/Paper_API-1.20.1-orange?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-red?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-purple?style=for-the-badge)
![Mobs](https://img.shields.io/badge/upgraded_mobs-7-yellow?style=for-the-badge)

A Minecraft Paper plugin that makes hostile mobs smarter and more dangerous. Each mob type gets unique tactical AI that goes far beyond vanilla behavior.

## Features

### 🩸 Bleeding Mechanic
Players have a configurable chance to start "bleeding" when hit by any hostile mob:
- **Redstone Particles**: Bleeding players trail red dust particles at their feet
- **Duration**: Lasts 10 seconds by default (configurable)
- **Trigger**: 15% chance on any monster hit (configurable)

### 🧟 Zombie — Block Breaker & Blood Tracker
Zombies can break through solid blocks to reach their target:
- Detects when path is blocked and begins "mining" the obstacle
- Shows visual block cracking overlay to nearby players
- Plays correct break sound for each block type
- **Protected blocks**: Bedrock, Barrier, Obsidian, End Portal Frames, Command Blocks, and Doors
- **Smell Blood**: Detects bleeding players from up to **64 blocks** away
- **Blood Lust**: Gains a speed boost when chasing a bleeding target

### 💥 Creeper — Stealth Stalker
Creepers behave like ambush predators:
- **Stalking**: Moves faster when the player isn't looking
- **Caution**: Slows down when the player is watching
- **Surprise Attack**: Ignites with shortened fuse when behind the player

### 🏹 Skeleton — Tactical Shooter
Skeletons become competent ranged fighters:
- **Take Cover**: Seeks positions behind blocks when player is looking at them
- **Strafe & Dodge**: Strafes sideways unpredictably when player draws a bow
- **Ground Validation**: Only moves to safe positions (solid ground, no walls)

### 🕷️ Spider — Ambush Predator
Spiders use their unique abilities more effectively:
- **Web Trap**: Places temporary cobwebs when fleeing at low health (auto-removed after 5s)
- **Ceiling Ambush**: Drops from ceilings onto players below at night
- **Leap Attack**: Pounces at players from distance when not being watched

### 👻 Phantom — Coordinated Dive Bomber
Phantoms attack as a coordinated pack:
- **Coordinated Strikes**: Multiple phantoms synchronize their dives to hit simultaneously
- **Speed Burst**: Gains speed during dives when the player isn't looking up
- **Persistence**: Doesn't break pursuit as easily after being hit

### 🟣 Enderman — Strategic Teleporter
Endermen use teleportation tactically:
- **Flank Teleport**: Teleports behind the player instead of randomly
- **Block Weaponization**: Places carried blocks in the player's path to obstruct movement

### 🧪 Witch — Combat Alchemist
Witches become dangerous kiting fighters:
- **Kiting**: Actively retreats when player gets too close, maintaining throwing distance
- **Self-Heal Priority**: Drinks healing potions more aggressively when below 50% health
- **Fire Resistance**: Automatically applies fire resistance when on fire

## Installation

1. Download `upgradedhostile-1.0.0.jar` from the `target/` directory.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Adjust `plugins/UpgradedHostile/config.yml` to taste and reload.

## Configuration

Every mob can be **individually enabled/disabled** and all behavior parameters are tunable in `config.yml`.

### Toggles
| Mob | Config Key | Default |
|---|---|---|
| Zombie | `zombie.enabled` | `true` |
| Creeper | `creeper.enabled` | `true` |
| Skeleton | `skeleton.enabled` | `true` |
| Spider | `spider.enabled` | `true` |
| Phantom | `phantom.enabled` | `true` |
| Enderman | `enderman.enabled` | `true` |
| Witch | `witch.enabled` | `true` |

See `config.yml` for the full list of tunable parameters per mob.

## Requirements

- **Paper** 1.20.1+ (or compatible fork)
- **Java** 17+

## Building

```bash
mvn clean package
```

The compiled JAR will be in `target/upgradedhostile-1.0.0.jar`.
