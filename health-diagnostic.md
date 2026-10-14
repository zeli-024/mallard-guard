# Source health review — 1.56a

The server owns eligibility, damage, successful parries, retaliation and restrictions. Local guarding remains presentation prediction; this release adds no latency compensation. Guard input handling, Better Combat suppression, Punchy playback, particle physics and artwork retain their previous implementations.

## Effects

Before Highlight uses its own lifetime regardless of hitlag. After retains a two-tick delay following hitlag and eligible White/Meme Flash. Hitlag, Impact Frame, Highlight and flash switches still gate their own work. The silhouette capture remains outside the entity renderer, preserving original material alpha and restoring GPU state. Existing native target reuse, idle disposal and session cleanup remain.

## Pose Undo

The header Undo restores a detached copy of the last saved manager preset, including guarding/recoil arrays, timing and assignment lists. A new preset falls back to its initial draft. It neither mutates the saved baseline nor writes files. Preview state is restarted while tab, scroll and manual opacity remain. Undo is unavailable in read-only mode and while unchanged. Existing partial Restore controls remain.

## Icon placement

The placement editor holds a detached five-setting draft. Apply uses the existing save transaction; Cancel/Discard preserve live values. Server locks are rechecked while editing and before applying. The screen uses existing widgets/artwork and the shared preview fade/Hide UI path. Opaque controls skip capture work; faded controls use the existing reusable targets and close/resize cleanup. Blur remains Off. Custom coordinates scale with available screen space.

Anchors read visible boss-bar positions, shared HUD heights and the vanilla offhand slot. Optional integrations report additional visible bounds for the current frame. Reports retain only integers and reset each frame. No registry scan, entity history, persistent callbacks or screen capture is added. HUDs drawn independently of these spacing systems need integration or manual positioning.

## Defaults and persistence

Player and shield windows are 2 perfect ticks within 4 total ticks. Hitlag uses 3 perfect/1 regular ticks with Perfect Only Off. Experimental Highlight remains Off; its defaults are Perfect Only On, Before, 6 ticks, 60% dim and 80% glow. The shield icon defaults On at Bottom Center, with 100% size, with regular/perfect/block expansions Off and guard-break expansion, Lift and existing echoes retained. Mob spark colors default white; Flying Sparks and Spark Debris have separate server-owned color controls. Saved custom settings remain intact.

Config revision 101 and preset revision 95 retain old keys and slot positions. Five HUD slots are appended; old preset lengths fill them from defaults. Matching clients/server use protocol 75. Existing config transfer bounds and save rollback remain in place.

## Verification and limits

Source checks cover Java grammar/literals, imports, local signatures, payload arities and codec order, effect eligibility/timing, permissions, shared UI geometry, anchors and normalized positions. Archive CRC, exact bytes and source-overlay cleanup are checked before delivery. These checks do not replace Java compilation, GPU testing, multiplayer testing or long-session heap profiling.

Test Reset defaults; independent Before duration with hitlag shorter/longer/Off; After with flash On/Off; both icon editor entry points; drag/resize/Cancel/Apply; boss-bar and offhand visibility changes; modded HUD spacing; green compact On buttons and neutral Enabled buttons.
