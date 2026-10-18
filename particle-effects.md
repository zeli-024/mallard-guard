# Particle effects

Flying Sparks, Spark Debris and Spark Tracers share a parry origin, an upward-favored spherical launch distribution and one bounded head budget (512 heads per burst, 1024 active). Shield Forward Bias, inside Spark Tracers and On by default, favors the defender's facing while keeping side and rear directions; it affects all three families. Fall parries use a horizontal ring with slight vertical variation instead. Each family has its own motion, drag, gravity, collisions and rendering.

## Amount

Each family has Base Intensity, Perfect Intensity and Damage Scaling.

- **Base Intensity** is a percentage of 100 heads. 0% disables that family for regular and perfect parries.
- **Perfect Intensity** adds a percentage of the base amount on perfect parries, up to 200%. 100% doubles the base.
- **Damage Scaling** adds a percentage of the selected amount per intercepted damage point. Projectile hits use an estimate when exact damage is unavailable.

Defaults for all three families: 6% Base, 125% Perfect and 2% Damage Scaling.

## Appearance

| | Flying Sparks | Spark Debris | Spark Tracers |
| --- | --- | --- | --- |
| Size | 80% | 100% | 125% |
| Lifetime | 20 ticks | 30 ticks | 12 ticks |

Flying Sparks normally live for 50–100% of their lifetime; 12% linger for 110–135%. Flying Size Variance defaults to 50%, giving a 0.55–1.60 spawn-size range. Spark Tracer tails last 1–4 ticks (default 2) and are drawn as tick-stepped squares.

Each family and the Center Impact has its own On/Off switch in its section header. Turning one off stops spawning without changing its saved sliders.

## Center Impact

A stationary, camera-facing six-frame animation made of six 64×64 white artwork frames and six matching tint masks. Defaults: Size 250%, Spin Speed 45°/second, Opacity 100%, Duration 8 ticks. The starting rotation and spin direction are chosen once per appearance. Expansion and dissipation are drawn into the frames; the runtime rotates the animation and fades its final frame without resizing it. Mask alpha controls color coverage. See `resource-pack-customization.md` to replace the artwork.

## Colors

All player and mob spark colors default to white (#FFFFFF). Spark Tracers use a Start/Middle/End palette across their lifetime, and the Center Impact uses the same palette. Regular and perfect parries share it.

Player colors (Flying Sparks, Spark Debris and the tracer palette) are shared with other players when joining and when a changed palette is applied. The server stores one palette per connected player, and each burst references it by a small numeric ID. Late joiners receive the current palettes. Palettes survive respawns and dimension changes and are removed on logout and server shutdown. Each burst captures its colors before any hitlag delay, so separate bursts never share mutable color state. Only colors are shared: each viewer keeps their own intensity, size, lifetime and On/Off preferences unless the server enforces that category.

Server enforcement overrides shared player palettes; disabling enforcement restores the players' own colors. Mob spark colors and their Center Impact always come from server settings, with separate Flying Sparks and Spark Debris colors and a tracer palette captured with each mob burst. Server config reloads redistribute the policy and cached palettes.

## Rendering and lifecycle

All effect geometry is drawn in the shared particle-atlas batch. Active lists are bounded and cleared on world and session changes. Death clears player-specific HUD feedback without removing other players' world effects. One shared monitor guards Mallard Guard's particle ticks, histories, rendering, palette publication and collision profiling, so its particles remain safe when another mod ticks particles asynchronously. Other mods' particle settings are not changed.
