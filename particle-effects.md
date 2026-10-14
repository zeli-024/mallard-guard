# Particle effects — current behavior

Flying Sparks, Spark Debris and Spark Tracers share a parry origin, an uneven upward-favored spherical launch distribution and one bounded head budget. Shield Forward Bias optionally favors defender yaw while retaining side/rear directions. Fall parries use a horizontal ring with slight vertical variation instead. Each family retains its own motion, drag, gravity, collisions and rendering.

Base lifetimes default to 20 ticks for Flying Sparks, 30 ticks for Spark Debris, and 12 ticks for Spark Tracers. Flying Sparks normally live for 50–100% of that value; 12% sample a longer 110–135% lifetime. This lifetime behavior is unchanged. Flying Size Variance now defaults to 50% while retaining the selected 0.55–1.60 spawn-size range. Base intensity defaults to 5%, perfect adds 50% of that base, damage adds 5% per damage point. Base intensity 0 disables the family. Tracer tails keep their independent 1–4-tick length setting and tick-stepped squares.

Center Impact is a stationary, camera-facing six-frame animation: six 64×64 white artwork PNGs and six matching tint-mask PNGs. Defaults: Size 250%, Spin Speed 45 degrees/second, Opacity 100%, Duration 8 ticks. Starting rotation and spin direction are sampled once per spawn. Drawn expansion and outward dissipation live in the PNG sequence; runtime rotates it and fades its final frame interval without resizing it over time. Mask alpha controls color coverage with no separate tint-strength slider.

All effect geometry shares the existing particle-atlas batch. Active lists are bounded and clear on world/session changes. Death clears player-specific HUD and feedback without deleting other players' world effects. One shared monitor protects custom rendering, particle ticks/history, registry publication/pruning, palette application, removal and collision-profiler windows. It supports overlapping tick/render scheduling without copying lists or trails every frame. Only Mallard Guard particle work is serialized; other mods' particle settings are not changed. Runtime compatibility still needs an AsyncParticles test.

Mob tracers and their Center Impact use the server-selected mob Start/Middle/End palette captured with the burst. Player palettes are shared and cached by the server. The palette is captured before the first tick; separate bursts never share mutable color state. Flying Sparks and Spark Debris retain their usual local colors.

Default player tracer/impact palette: #FFEBD7 → #E9B692 → #FFFFFF. Default mob palette: #FFE7E3 → #F20900 → #B9B9B9. Flying Sparks and Spark Debris remain white. Chromatic Aberration defaults to 6 ticks.

## Multiplayer palettes and effect toggles

Player colors (Flying Sparks, Spark Debris and one shared three-color Spark Tracer palette) are shared on joining and applying a changed palette. The server stores one palette per connected player; bursts reference its small numeric ID. A late join receives the current palettes. Respawns and dimension changes retain the palette; logout and server shutdown remove it. Each received burst captures immutable colors before any hitlag delay, and each spawned particle retains that selection. Only colors are shared; the viewer keeps their local intensity, size, lifetime and enable/disable preferences unless the server enforces that category.

Server enforcement overrides shared player palettes; disabling enforcement restores their submitted colors. Mob tracer colors and matching impact colors always come from server settings. Server config reloads redistribute the policy and cached palettes.

Each spark family and the Center Impact has its own On/Off switch in its existing divider. Off stops spawning that family without changing its saved sliders. The shield forward-bias option now sits inside Spark Tracers and defaults On; it still affects all three spark families. Fall parries retain their horizontal ring spread.

Hitlag durations use Minecraft ticks. Perfect defaults to 8 ticks, regular to 0. The shared Hitlag switch and Perfect Only option apply to both parrying and getting parried. Historical 1/60-second durations migrate to ticks once; reset uses the current defaults.

Regular and perfect parries share the same Start/Middle/End tracer palette and matching Center Impact colors. The old perfect-only palette positions remain reserved in serialized presets; their config keys are retired. Mob colors remain server-owned.

Impact Frame runs during the latter half of hitlag when a freeze plays. Without hitlag it filters the live world using the existing Hitlag Duration (regular zero falls back to that duration); no freeze is added. It defaults Off / Perfect Only On. Target Highlight defaults to Before, Perfect Only, 6 ticks, 60% world dim, 2-tick fades and 80% glow; attacker white, target red. Before starts with feedback and always uses its own lifetime, independently of hitlag. White Flash defaults Off, starts at hitlag end (immediately without hitlag), and keeps its 260/190 ms fades. After-mode Highlight starts two ticks after eligible White/Meme Flash ends, or two ticks after hitlag when no flash plays. Camera Shake and Chromatic Aberration do not delay this sequence. Their regular controls follow the unsaved Perfect Only draft immediately.

All spark families default to 8% Base, 32% Perfect and 2% Damage Intensity. Player colors default white; the server-owned mob colors default red and tint every family plus Center Impact.

Hitlag defaults to 3 ticks for perfect and 1 tick for regular parries. For the fullest Impact Frame presentation, set perfect hitlag to 6 ticks. This is a recommendation, not an automatic change when enabling Impact Frame.
