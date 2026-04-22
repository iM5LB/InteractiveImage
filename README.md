# InteractiveImage

InteractiveImage is a Paper plugin that turns **ImageFrame** (LOOHP) images into **interactive UI panels**.

ImageFrame renders images/GIFs in item frames; InteractiveImage adds:
- hover detection
- highlight effects (glow)
- HUD effects (ActionBar / Title / BossBar)
- click actions (left/right)

## Requirements
- Paper `1.21+` (or any Paper-based fork)
- ImageFrame by LOOHP (required)

## Install
1. Put `InteractiveImage-1.0.0.jar` in your server `plugins/` folder.
2. Restart the server.

## Commands
- `/ii` - toggle editor mode (ON/OFF)
- `/ii on` / `/ii off` - set editor mode
- `/ii reload` - reload data + restart scanners

Aliases: `iimage`, `iiimage`, `interactiveimage`

## Permission
- `interactiveimage.admin`

## In-game editor
1. Run `/ii` to enable editor mode.
2. Look at an ImageFrame item frame and **right-click** it to open the GUI.
3. Configure effects, activation distances, visibility, and click actions.

## Actions format
Actions are stored as strings. Examples:
- `console:say hello`
- `player:say hello`

## Data
- Rules are stored in `plugins/InteractiveImage/iiamge.json`.
- No `config.yml`.

## Notes / limitations
- Colored **FRAME** glow uses a scoreboard team-color fallback if the server build does not support entity glow color overrides. This can conflict with other plugins that manage entity teams.
- **BLOCK** glow uses a glowing display of the attached block; appearance can vary with shaders/resource packs.

## Build (developers)
Run: `./gradlew build`

