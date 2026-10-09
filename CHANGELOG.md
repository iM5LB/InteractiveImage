# Changelog

## [1.0.4-alpha] - 2026-08-11

### Fixed
- **Player & Proxy Commands** — fixed `player:` action execution to use `player.performCommand()`, ensuring network proxy commands (e.g. BungeeCord / Velocity `/server <target>`) and command aliases work as expected
- **Protected Region Left-Clicks** — removed `ignoreCancelled = true` on entity damage listener and ensured damage is cancelled on interactive frames, allowing left-clicks in protected spawn/lobby areas without breaking item frames
- **Air & Block Click Detection** — resolved close-range click drop in `FocusedAirClickListener` and added `PlayerAnimationEvent` arm-swing handling for reliable left-clicks at any distance

---

## [1.0.3-alpha] - 2026-05-13

### Added
- **Update Notifier** — automatically checks for new plugin versions on startup
- Notifies admins in-game when an update is available (with 24-hour cooldown)
- Config option to enable/disable update checking (`check-for-updates: true`)
- Debug logging for troubleshooting update checks

---

## [1.0.2-alpha] - 2026-05-10

### Added
- Image Swap — hover a frame to display a different map, with auto-revert and revert-on-unfocus options
- Extended-range clicking — click actions now fire beyond vanilla 3-block reach
- Wildcard rule (`*`) — applies to all maps with no explicit rule
- `/ii enable|disable <mapName>` — toggle a map rule from command line

---

## [1.0.1] - 2026-05-06

### Fixed
- Glow BLOCK mode: frame disappears on hover
- Glow BLOCK mode: grid outline on transparent blocks
- Click actions not firing from beyond vanilla reach
- Click actions firing twice (off-hand / close-range double-fire)

---

## [1.0.0] - Initial Release
- Hover detection, glow, glint, ActionBar, Title, BossBar effects
- Left/right click actions with `console:` and `player:` prefixes
- Multi-frame image grouping
- In-game editor GUI via `/ii`
