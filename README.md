# InteractiveImage

A Paper plugin that turns [ImageFrame](https://www.spigotmc.org/resources/imageframe.106031/) maps into interactive UI panels — hover detection, visual effects, and click actions, all configured in-game.

<div align="center">

<img src="https://github.com/iM5LB/InteractiveImage/blob/main/interactiveImage.gif?raw=true">

</div>

## Requirements

| Dependency | Version |
|---|---|
| Paper (or fork) | 1.21+ |
| ImageFrame by LOOHP | latest |
| Java | 21+ |

## Installation

1. Drop `InteractiveImage.jar` into your server's `plugins/` folder.
2. Make sure **ImageFrame** is installed.
3. Restart the server.

No `config.yml` is generated. All settings are per-frame and stored in `plugins/InteractiveImage/iiamge.json`.

## Commands

| Command | Description |
|---|---|
| `/ii` | Toggle editor mode on/off |
| `/ii on` / `/ii off` | Explicitly set editor mode |
| `/ii reload` | Reload data and restart scanners |

**Aliases:** `iimage`, `iiimage`, `interactiveimage`  
**Permission:** `interactiveimage.admin` (default: op)

## In-Game Editor

1. Run `/ii` to enter editor mode.
2. Look at an ImageFrame item frame and **right-click** it to open the GUI.
3. Configure the frame's effects, activation distances, visibility, and click actions.
4. Run `/ii` again to exit editor mode.

## Effects

Each frame can have any combination of these effects applied when a player looks at it:

- **Glow** — highlights the frame with a configurable color and mode (`FRAME` or `BLOCK`)
- **Glint** — item-enchantment-style shimmer on the frame
- **Hidden frame block highlight** — reveals the block behind a hidden item frame
- **ActionBar** — displays a message in the action bar with a configurable format and refresh rate
- **Title** — sends a title/subtitle with configurable fade-in, stay, and fade-out durations
- **BossBar** — shows a boss bar with configurable text, color, style, and progress

All effects support per-frame overrides on top of server-wide defaults.

## Click Actions

Actions fire when a player left-clicks or right-clicks a configured frame. Each action is a string in one of these formats:

| Format | Behavior |
|---|---|
| `console:<command>` | Runs the command as the console |
| `player:<command>` | Runs the command as the clicking player |
| `server:<name>` | Sends the player to another server on the proxy (e.g. `server:survival`) |
| `<command>` | Same as `player:` |

Actions support placeholders that are replaced at runtime (e.g. player name, map name).

**Example actions:**
```
console:say %player% clicked the map
player:warp spawn
server:survival
```

> `server:` sends a proxy transfer request via the BungeeCord plugin
> message channel, so it works on BungeeCord and Velocity (legacy
> channel). Use it instead of `player:server <name>` — backend
> `performCommand` never reaches proxy commands like Velocity `/server`.

## Per-Frame Configuration

Each frame rule supports:

- `title` — display name
- `cooldownTicks` — cooldown between action triggers
- `cancelInteract` — whether to cancel the interact event
- `onRightClick` / `onLeftClick` — action lists
- `activation` — override hover distance, click distance, and hover-required-for-click
- `effects` — override glow color/mode, frame visibility, ActionBar, Title, and BossBar settings

## Building

```bash
./gradlew build
```

Output jar will be in `build/libs/`.
