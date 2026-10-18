# Resource-pack customization

All paths below are inside `assets/mallardguard/` in a resource pack. Replace assets at the same path to change their appearance or sound. These are client resources, not gameplay rules.

| Feature | Files | Paths |
| --- | ---: | --- |
| Center Impact artwork | 6 PNGs | `textures/particle/center_impact/animation/frame_0.png` through `frame_5.png` |
| Center Impact tint selection | 6 PNGs | `textures/particle/center_impact/tint/frame_0.png` through `frame_5.png` |
| Shield HUD icons | 2 PNGs | `textures/gui/shield.png`, `textures/gui/shield_red.png` |
| Spark Debris point | 1 PNG | `textures/particle/dot_streak_point.png` |
| Spark Tracer point | 1 PNG | `textures/particle/shooting_spark_point.png` |
| Meme flashes | 4 PNGs | `textures/gui/meme_flash_1.png`, `meme_flash_2.png`, `meme_flash_3.png`, `meme_flash_plankton.png` |
| Config-menu logo | 1 PNG | `textures/gui/mod_logo.png` |
| Audio | 50 OGGs and 1 JSON | `sounds/` and `sounds.json`; 25 recordings and their 25 boosted alternatives |
| Translated text | 1 JSON | `lang/en_us.json`; key names and compatibility notices only. Most menu labels are currently Java literals. |
| Particle texture definitions | 4 JSONs | `particles/flying_spark.json`, `dot_streak.json`, `shooting_spark.json`, `center_impact.json` |

There are 22 mod-owned texture PNGs and 50 OGG files. The four particle definitions, sound definitions and language file add six JSON resources. Flying Sparks also use nine vanilla sprites: `minecraft:spark_7` through `minecraft:spark_0`, plus `minecraft:glow`. You can redirect the Flying Sparks definition to your own pack textures to avoid changing other vanilla effects.

## Center Impact

Each impact sits at the particle burst origin, faces the camera, and has no movement or collision. Its white artwork contains a two-cross opening and outward dissipation. All six frames are 64×64 and use one shared center. The runtime chooses one random starting angle and one spin direction, advances through the six frames across Duration, and applies a final fade. It does not scale the artwork over time.

Each frame has a corresponding mask. The white artwork is drawn first; a colored copy of the mask is drawn above it unless the current color is white. Mask alpha selects how much color covers each area. Keep the mask within the artwork silhouette and preserve its transparent holes. The color moves through the tracer Start, Middle and End palette across normalized lifetime. Mob impacts use the palette sent with the mob's burst. Overall Opacity scales both draws.

The code expects six artwork sprites followed by six matching masks in `particles/center_impact.json`. Preserve that count and order when replacing the animation. Packs can change pixels, transparency and tint selection; Size, Spin Speed, Opacity and Duration remain config controls. At most two quads are drawn per impact, in the shared particle-atlas batch; an all-white impact uses one.

## Shield icons

Both shield files are static 16×16 images. The code reuses them for the guarding/recharge meter and for regular parry, perfect parry, block, guard-break and ready reactions. Cropping, scaling, shaking, tinting, flashing, expanding echoes and fading are calculated in Java, not stored as PNG animation frames.

On resource reload, `GuardShieldArtwork` reads the active pack textures and caches their nontransparent coverage on a 16×16 grid. White flashes and the ready outline follow that coverage. Higher-resolution replacements still use the existing HUD coordinate system and 16×16 coverage mask. A resource pack can change the icon shape and artwork, but cannot replace the reaction choreography or timing through these two images.

Menu borders, color-picker geometry, the impact screen filter and Punchy pose presets are implemented in code/config rather than resource-pack assets.
