# Source responsibilities

Related responsibilities have explicit owners. Assignment policy, persisted metadata and rendering remain separate from editable screen state and packet transport state.

| Area | Owner | Responsibility |
| --- | --- | --- |
| Combat | GuardState, GuardCombatEffects, GuardRetaliation, GuardMobState | Guard windows, damage outcomes, retaliation and mob behavior |
| Config declarations | GuardConfig, GuardClientSettings, particle config classes | Persisted keys, defaults, ranges and client category membership |
| Item assignments | GuardItemRules, GuardItemAssignments, GuardItemBlockCounts | Compiled policy, live group membership, staged rule drafts and exact block-count overrides |
| Numeric metadata | GuardSettingRanges, GuardControlSettings | Shared declared bounds, stable control identities, display scaling and drag precision |
| Config compatibility | GuardConfigMigration, GuardPresetMigration | Historical default notices, old file keys and serialized preset formats |
| Config transfer | GuardConfigTransfer | Size-limited uploads/snapshots, timeouts and session cleanup |
| Wire payloads | GuardPackets | Packet records, codecs, handlers and validated saves |
| Server colors | GuardPlayerPalettes | Per-player palettes, enforcement and join/leave publication |
| Guard presentation | GuardPrediction, GuardClient | Local prediction, input IDs, reply correction, poses and meter |
| Client policy | GuardClientPolicy | Personal/enforced settings and connection-scoped palette cache |
| Config draft | GuardConfigDraft | Explicit read/write bindings, saved snapshots and save-group ownership |
| Config pages | GuardConfigPages, GuardConfigSearch | Page content and immutable search catalogue |
| Assignment UI | GuardPoseItemScreen, GuardItemSearch, GuardPoseOwnershipScreen | Shared grid, bounded indexing/bulk work, tooltips and exclusive-ownership review |
| Icon placement | GuardIconLayout, GuardIconPositionScreen | Frame-local HUD reservations, screen anchors and a detached draggable position draft |
| Screen feedback | GuardScreenSettings, GuardHitlag, GuardParryHighlight, GuardImpactFrame | Shared metadata, feedback timing, participant masks and fullscreen drawing |
| Main screen | GuardConfigScreen | Navigation, common row controls, permissions, undo and save acknowledgements |
| Shared UI | GuardUi, GuardUiLayout, GuardSlider | Theme, field/button creation, layout, scrolling, tooltips and slider behavior |
| Particles | Existing particle behavior, history, spawner and renderer classes | Launch physics, bounce, animation, bounded histories and one atlas batch |
| Compatibility | GuardPunchyApi, PunchyGuardCompat, GuardBetterCombatCompat, GuardShieldExpansionCompat, mixins | External APIs and renderer hooks |

Use explicit `Control` or preset-slot bindings when adding a control. Display labels are presentation; they must not resolve a config value. Declare numeric bounds through GuardSettingRanges so controls and validation share them. Keep historical preset positions reserved. Add new drafts to GuardConfigDraft with their correct save groups. Editors use shared widgets, while retaining specialized previews and item-grid behavior.

Saved custom values remain intact. Reset uses current defaults. Server edits remain operator-only, and forced client categories remain locked. Client and server must run the same release.

## Local guard prediction

`GuardPrediction` holds one local presentation state and monotonically increasing input IDs. `GuardClient` applies that state to poses, shield use and the meter. `GuardState` validates each request and replies with the matching ID through `GuardPackets`. Prediction never applies damage or grants a successful parry.

## Better Combat swing ownership

`GuardBetterCombatCompat` reads Better Combat's actual swing and feint state. Existing attack readiness checks gate guard entry. Vanilla and Better Combat attack starts share `GuardClient.attacksBlocked`: active guarding or authoritative incoming stun prevents a new attack. Releasing guard permits attacks when no stun remains. Incoming parries use the adapter's optional cancellation hook; this does not reintroduce the retired guard-to-attack handoff attempts.

## Pose editing and preset deletion

The pose editor stores only its editable draft and UI navigation. GuardUiLayout owns geometry; shared widgets own styling and precision. Pose, Timing and Items are separate workspaces; guarding and parried stages share assignments. Deleting a built-in preset stores a disabled tombstone so the five legacy slot identities remain stable. The editor header Undo restores the entire last saved preset without committing; partial Restore controls remain scoped to the current part, timing or assignments. A new preset restores its initial draft. Custom presets are removed. Tombstones are hidden from browsers and cannot retain exclusive assignments; a factory reset restores defaults.

## Highlight capture and sequencing

The living-render mixin copies only the selected opponent's submitted vertices into per-material batches. It does not allocate, clear or bind GPU targets. GuardParryHighlight performs mask work afterward inside GuardRenderTargets.State, preserving the original renderer state and material alpha. Targets are reused between nearby parries and retired after idle/session cleanup.

GuardHitlag advances the highlight state once per rendered feedback frame. Before-mode Highlight starts with feedback and uses its own configured duration, with or without a freeze. White Flash starts at the end of hitlag; After-mode Highlight waits for that flash, including Meme Flash when enabled, then delays two ticks. Camera Shake and Chromatic Aberration run independently. Impact Frame can filter a live scene with hitlag disabled. No registry scan or full entity rerender is added.

Diagnostics is listed under Server. Its local channels are grouped into Combat, Animations, Effects & Performance, and Config & Assignments. The shared channel definitions drive menu/search entries; event caches remain bounded. Server gameplay permissions are not granted by diagnostic access.

## Icon placement

Icon anchor, custom X/Y fractions, gap and size occupy five appended HUD preset slots. Existing preset positions do not move. Center preserves the original crosshair-relative placement. Dragged positions use 0–10000 fractions of the usable screen so GUI-scale and resolution changes preserve placement. The position editor uses GuardPreviewScreen for fading and Hide UI, previews the existing shield artwork without blur, pauses single-player, and commits through the normal config save transaction. Apply rechecks edit permission; Cancel/Discard do not mutate settings.

GuardIconLayout reads the current frame’s boss-bar positions and NeoForge left/right HUD stack heights. Hotbar anchors account for the vanilla offhand slot. Extra-slot integrations can call reserveHotbar with visible GUI bounds; independently positioned bottom bars can call reserveBottom with their upper edge. These reports expire at the next RenderGuiEvent.Pre, and must occur before the LOWEST-priority Post pass. No entity references, persistent provider callbacks, registry scanning or screen-pixel analysis are retained. Mods that neither use shared spacing nor report bounds require manual Gap adjustment or dragging.

Bottom Center is the default icon anchor. Icon size defaults to 100% of the original 18 GUI pixels and ranges from 50–200%. The same scaled dimensions drive the preview, collision-free screen bounds and live HUD placement.
