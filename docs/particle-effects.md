# Particle effects

Flying Sparks, Spark Debris and Spark Tracers scatter from the parry point, mostly upward. Shield Forward Bias favors the defender's facing for all three effects; fall parries scatter in a horizontal ring. Each effect has its own motion, gravity and collisions.

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

Each effect has an On/Off switch in its section header.

## Center Impact

A stationary, camera-facing six-frame animation made of six 64×64 white artwork frames and six matching tint masks. Defaults: Size 250%, Spin Speed 45°/second, Opacity 100%, Duration 8 ticks. The starting rotation and spin direction are chosen once per appearance. Expansion and dissipation are drawn into the frames; the runtime rotates the animation and fades its final frame without resizing it. Mask alpha controls color coverage. See `resource-pack-customization.md` to replace the artwork.

## Colors

All player and mob spark colors default to white (#FFFFFF). Spark Tracers use a Start/Middle/End palette across their lifetime, and the Center Impact uses the same palette. Regular and perfect parries share it.

Other players see your chosen colors. Each viewer controls particle amounts, sizes, lifetimes and visibility unless the server overrides them.

Server overrides can replace player colors. Mob colors always come from server settings, including separate Flying Sparks and Spark Debris colors and a tracer palette shared with Center Impact.
