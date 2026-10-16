#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
# Remove retired comparison files left by an older source overlay.
rm -f -- src/main/java/dev/zeli/mallardguard/client/GuardTerramitySparkParticle.java
rm -f -- src/main/resources/assets/mallardguard/particles/aurukel_spark.json
rm -f -- aurukel-particle-tuning.md
rm -f -- src/main/java/dev/zeli/mallardguard/GuardShootingSparkConfig.java
rm -f -- src/main/java/dev/zeli/mallardguard/client/GuardShootingSparkParticle.java
rm -f -- shooting-spark-tuning.md
# Remove Continuous Guard input interception from older source overlays.
rm -f -- src/main/java/dev/zeli/mallardguard/mixin/client/MinecraftGuardAttackMixin.java

# Swing state and feint handling now share the Better Combat adapter.
rm -f -- src/main/java/dev/zeli/mallardguard/client/GuardFeint.java
rm -f -- src/main/java/dev/zeli/mallardguard/mixin/client/BetterCombatFeintMixin.java

# Retired impact components left by an older ZIP overlay.
rm -rf -- src/main/resources/assets/mallardguard/textures/particle/center_impact/ring
rm -rf -- src/main/resources/assets/mallardguard/textures/particle/center_impact/cross
rm -rf -- src/main/resources/assets/mallardguard/textures/particle/center_impact/layers
for frame in 0 1 2 3 4 5; do
    rm -f -- "src/main/resources/assets/mallardguard/textures/particle/center_impact/frame_$frame.png"
done
rm -f -- art-source/original-impact-reference.png art-source/approved-cross-impact-reference.png
rm -f -- art-source/ImpactLayerExport.java art-source/ring-impact.png art-source/cross-impact.png
rm -f -- art-source/cross-generated-reference.png art-source/ring-generated-reference.png
if [[ -d art-source ]]; then
    find art-source -maxdepth 1 -type f -name '*prompt*' -delete
fi
rm -f -- release-notes/1.54c-beta-11-draft.md

# Retired mob blocking and regular-window config keys.
# GuardConfig removes retired config keys when saved.

# Detailed mob combat settings remain available in the server file, but are hidden in the UI.
# New gear rules and mob tracer colors are migrated by GuardConfig. No live files are retired.

# Remove retired textures left by an older source overlay.
rm -rf -- src/main/resources/assets/mallardguard/textures/particle/parry_disc

rm -f src/main/resources/assets/mallardguard/textures/gui/block_vignette.png

# Superseded local preview notes.
rm -f -- release-notes/1.56b-beta-1.md
