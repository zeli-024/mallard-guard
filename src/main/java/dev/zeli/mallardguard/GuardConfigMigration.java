package dev.zeli.mallardguard;

import static dev.zeli.mallardguard.GuardConfig.*;
import java.util.*;
import com.electronwill.nightconfig.core.Config;

/** Historical default notices and old-file migrations; live behavior stays in the config. */
final class GuardConfigMigration {
    private GuardConfigMigration() {}
    private record DefaultChange(int revision, String path, String previous, String current) {}
    private static final List<DefaultChange> DEFAULT_CHANGES = List.of(
        new DefaultChange(105,"timing.perfectTicks","6","2"),
        new DefaultChange(105,"timing.parryTicks","9","6"),
        new DefaultChange(105,"shields.perfectTicks","6","2"),
        new DefaultChange(105,"shields.parryTicks","9","6"),
        new DefaultChange(104,"particles.flyingSparks.size","100","80"),
        new DefaultChange(104,"particles.flyingSparks.baseIntensity","8","6"),
        new DefaultChange(104,"particles.flyingSparks.perfectIntensity","32","125"),
        new DefaultChange(104,"particles.dotStreaks.size","120","100"),
        new DefaultChange(104,"particles.dotStreaks.baseIntensity","8","6"),
        new DefaultChange(104,"particles.dotStreaks.perfectIntensity","32","125"),
        new DefaultChange(104,"particles.shootingSparks.overallSize","150","125"),
        new DefaultChange(104,"particles.shootingSparks.baseIntensity","8","6"),
        new DefaultChange(104,"particles.shootingSparks.perfectIntensity","32","125"),
        new DefaultChange(103,"timing.perfectTicks","2","6"),
        new DefaultChange(103,"timing.parryTicks","4","9"),
        new DefaultChange(103,"shields.perfectTicks","2","6"),
        new DefaultChange(103,"shields.parryTicks","4","9"),
        new DefaultChange(103,"parried.guardBreakTicks","10","15"),
        new DefaultChange(103,"parried.stunTicks","10","15"),
        new DefaultChange(102,"mobs.tracerStartColor","16711680","16777215"),
        new DefaultChange(102,"mobs.tracerMiddleColor","16711680","16777215"),
        new DefaultChange(102,"mobs.tracerEndColor","16711680","16777215"),
        new DefaultChange(101,"display.hitlagTicks","6","3"),
        new DefaultChange(101,"display.iconAnchor","0","6"),
        new DefaultChange(100,"timing.parryTicks","6","4"),
        new DefaultChange(100,"shields.parryTicks","6","4"),
        new DefaultChange(100,"display.hud","false","true"),
        new DefaultChange(100,"display.hitlagPerfectOnly","1","0"),
        new DefaultChange(100,"display.targetHighlightTicks","12","6"),
        new DefaultChange(100,"display.targetHighlightDim","30","60"),
        new DefaultChange(100,"display.targetHighlightGlow","60","80"),
        new DefaultChange(100,"display.icon0Component0","1","0"),
        new DefaultChange(100,"display.icon1Component0","1","0"),
        new DefaultChange(100,"display.icon2Component0","1","0"),
        new DefaultChange(99,"display.hitlagTicks","8","6"),
        new DefaultChange(99,"display.regularHitlagTicks","0","1"),
        new DefaultChange(98,"display.targetHighlightEnabled","1","0"),
        new DefaultChange(97,"particles.flyingSparks.baseIntensity","5","8"),
        new DefaultChange(97,"particles.flyingSparks.perfectIntensity","50","32"),
        new DefaultChange(97,"particles.flyingSparks.damageScaling","5","2"),
        new DefaultChange(97,"particles.dotStreaks.baseIntensity","5","8"),
        new DefaultChange(97,"particles.dotStreaks.perfectIntensity","50","32"),
        new DefaultChange(97,"particles.dotStreaks.damageScaling","5","2"),
        new DefaultChange(97,"particles.shootingSparks.baseIntensity","5","8"),
        new DefaultChange(97,"particles.shootingSparks.perfectIntensity","50","32"),
        new DefaultChange(97,"particles.shootingSparks.damageScaling","5","2"),
        new DefaultChange(97,"display.hud","true","false"),
        new DefaultChange(97,"display.screenFlash","true","false"),
        new DefaultChange(97,"display.targetHighlightTicks","8","12"),
        new DefaultChange(97,"display.targetHighlightAfterHitlag","1","0"),
        new DefaultChange(97,"display.targetHighlightColor","16777215","16711680"),
        new DefaultChange(97,"particles.shootingSparks.startColor","16772055","16777215"),
        new DefaultChange(97,"particles.shootingSparks.middleColor","15316626","16777215"),
        new DefaultChange(97,"mobs.tracerStartColor","16771043","16711680"),
        new DefaultChange(97,"mobs.tracerMiddleColor","15862016","16711680"),
        new DefaultChange(97,"mobs.tracerEndColor","12171705","16711680"),
        new DefaultChange(97,"parriedValues","Automatic recoil offsets","Copy the guarding pose"),
        new DefaultChange(96,"defense.followUpParryTicks","3","5"),
        new DefaultChange(96,"display.impactFrames","1","0"),
        new DefaultChange(96,"display.hitlagTicks",null,"8 ticks"),
        new DefaultChange(96,"display.targetHighlightTicks",null,"8 ticks"),
        new DefaultChange(96,"display.targetHighlightDim",null,"30%"),
        new DefaultChange(96,"display.targetHighlightFadeInTicks",null,"2 ticks"),
        new DefaultChange(96,"display.targetHighlightFadeOutTicks",null,"2 ticks"),
        new DefaultChange(96,"display.targetHighlightGlow",null,"60%"),
        new DefaultChange(96,"parried.guardBreakTicks",null,"10 ticks"),
        new DefaultChange(96,"parried.stunTicks",null,"10 ticks"),
        new DefaultChange(90, "mobs.tracerStartColor", "7815994", "16771043"),
        new DefaultChange(90, "mobs.tracerMiddleColor", "10105901", "15862016"),
        new DefaultChange(90, "mobs.tracerEndColor", "13619151", "12171705"),
        new DefaultChange(89, "timing.perfectTicks", "1", "2"),
        new DefaultChange(89, "particles.flyingSparks.lifetimeTicks", "12", "20"),
        new DefaultChange(89, "particles.dotStreaks.lifetimeTicks", "12", "30"),
        new DefaultChange(88, "particles.shieldForwardBias", "0", "1"),
        new DefaultChange(88, "mobs.tracerEndColor", "6645093", "13619151"),
        new DefaultChange(87, "display.chromaticTicks", "10", "6"),
        new DefaultChange(87, "mobs.gearWhitelist", "All vanilla equipment", "40 vanilla armor/tool assignments"),
        new DefaultChange(87, "mobs.gearBlacklist", "#minecraft:axes", ""),
        new DefaultChange(87, "particles.shootingSparks.startColor", "16777215", "16772055"),
        new DefaultChange(87, "particles.shootingSparks.middleColor", "16777215", "15316626"),
        new DefaultChange(87, "mobs.tracerStartColor", "16777215", "7815994"),
        new DefaultChange(87, "mobs.tracerMiddleColor", "16777215", "10105901"),
        new DefaultChange(87, "mobs.tracerEndColor", "16777215", "6645093"),
        new DefaultChange(86, "mobs.perfectTicks", "10", "15"),
        new DefaultChange(86, "mobs.tacticalMaxSeconds", "1", "2"),
        new DefaultChange(86, "mobs.approachMaxSeconds", "1", "2"),
        new DefaultChange(86, "particles.flyingSparks.sizeVariance", "49", "50"),
        new DefaultChange(83, "timing.perfectTicks", "3", "1"),
        new DefaultChange(83, "timing.parryTicks", "8", "6"),
        new DefaultChange(83, "shields.perfectTicks", "1", "2"),
        new DefaultChange(83, "defense.parryRetaliation", "0.25", "0.4"),
        new DefaultChange(83, "defense.perfectRetaliation", "0.5", "0.8"),
        new DefaultChange(83, "sources.knockbackStrength", "80", "50"),
        new DefaultChange(83, "shields.regularPushback", "85", "100"),
        new DefaultChange(83, "mobs.perfectTicks", "20", "10"),
        new DefaultChange(83, "mobs.counterTicks", "12", "20"),
        new DefaultChange(83, "mobs.tacticalMaxSeconds", "5", "1"),
        new DefaultChange(83, "mobs.tacticalCooldownTicks", "200", "100"),
        new DefaultChange(83, "mobs.approachDistance", "2", "3"),
        new DefaultChange(83, "mobs.approachMaxSeconds", "2", "1"),
        new DefaultChange(83, "mobs.rushChance", "70", "100"),
        new DefaultChange(83, "particles.centerImpact.size", "100", "250"),
        new DefaultChange(83, "particles.centerImpact.spinSpeed", "90", "45"),
        new DefaultChange(83, "particles.centerImpact.durationTicks", "6", "8"),
        new DefaultChange(83, "particles.flyingSparks.lifetimeTicks", "36", "12"),
        new DefaultChange(83, "particles.dotStreaks.lifetimeTicks", "32", "12"),
        new DefaultChange(83, "particles.shootingSparks.lifetimeTicks", "16", "12"),
        new DefaultChange(82, "particles.centerImpact.spinSpeed", "0", "90"),
        new DefaultChange(80, "particles.flyingSparks.baseIntensity", "8", "5"),
        new DefaultChange(80, "particles.dotStreaks.baseIntensity", "8", "5"),
        new DefaultChange(80, "particles.shootingSparks.baseIntensity", "8", "5"),
        new DefaultChange(80, "particles.dotStreaks.size", "100", "120"),
        new DefaultChange(80, "particles.dotStreaks.baseColor", "16770741", "16777215"),
        new DefaultChange(80, "particles.shootingSparks.startColor", "16773326", "16777215"),
        new DefaultChange(80, "particles.shootingSparks.middleColor", "16760952", "16777215"),
        new DefaultChange(80, "particles.shootingSparks.endColor", "8419700", "16777215"),

        new DefaultChange(77, "particles.centerImpact.opacity", "65", "100"),
        new DefaultChange(77, "particles.centerImpact.fadeOutFrames", "1", "2"),
        new DefaultChange(76, "particles.flyingSparks.damageScaling", "25", "5"),
        new DefaultChange(76, "particles.dotStreaks.baseIntensity", "4", "8"),
        new DefaultChange(76, "particles.dotStreaks.damageScaling", "25", "5"),
        new DefaultChange(76, "particles.shootingSparks.baseIntensity", "12", "8"),
        new DefaultChange(76, "particles.shootingSparks.damageScaling", "25", "5"),
        new DefaultChange(73, "pose1", "1,17,13,-28,7,-14,74,10,25,-16,23,30,-18,0,0,0,36,-7,-19,300,360,2,150", "1,17,13,-28,7,-14,74,10,25,-16,23,30,-18,0,0,0,36,-7,-19,150,360,2,150"),
        new DefaultChange(73, "pose2", "1,91,10,-30,22,-6,-10,-5,5,3,10,-8,-18,0,0,0,55,9,13,300,360,2,150", "1,91,10,-30,22,-6,-10,-5,5,3,10,-8,-18,0,0,0,55,9,13,150,360,2,150"),
        new DefaultChange(73, "pose3", "1,8,100,-12,-23,11,54,-5,8,3,10,-8,-18,0,0,0,38,-29,11,300,360,2,150", "1,8,100,-12,-23,11,54,-5,8,3,10,-8,-18,0,0,0,38,-29,11,150,360,2,150"),
        new DefaultChange(73, "pose4", "1,68,19,-24,4,-33,80,-5,8,3,10,-8,-18,0,0,0,24,180,10,300,360,3,100", "1,68,19,-24,4,-33,80,-5,8,3,10,-8,-18,0,0,0,24,180,10,150,360,3,100"),
        new DefaultChange(73, "pose5", "1,39,3,-34,-39,-10,43,-26,24,45,-54,-11,-66,0,0,0,0,-8,10,300,360,2,100", "1,39,3,-34,-39,-10,43,-26,24,45,-54,-11,-66,0,0,0,0,-8,10,150,360,2,100"),
        new DefaultChange(73, "particles.flyingSparks.damageScaling", "0", "25"),
        new DefaultChange(73, "particles.dotStreaks.perfectIntensity", "100% increase", "50% increase"),
        new DefaultChange(73, "particles.dotStreaks.damageScaling", "4", "25"),
        new DefaultChange(73, "particles.shootingSparks.baseIntensity", "16", "12"),
        new DefaultChange(73, "particles.shootingSparks.perfectIntensity", "0% increase", "50% increase"),
        new DefaultChange(73, "particles.shootingSparks.damageScaling", "0", "25"),
        new DefaultChange(72, "particles.shootingSparks.overallSize", "100%", "150%"),
        new DefaultChange(70, "particles.flyingSparks.perfectIntensity", "12 heads", "50% increase"),
        new DefaultChange(70, "particles.dotStreaks.perfectIntensity", "12 heads", "100% increase"),
        new DefaultChange(70, "particles.shootingSparks.perfectIntensity", "16 heads", "0% increase"),
        new DefaultChange(69, "particles.flyingSparks.baseIntensity", "12", "8"),
        new DefaultChange(69, "particles.flyingSparks.perfectIntensity", "16", "12"),
        new DefaultChange(69, "particles.dotStreaks.baseIntensity", "12", "4"),
        new DefaultChange(69, "particles.dotStreaks.perfectIntensity", "8", "12"),
        new DefaultChange(66, "particles.shootingSparks.launchSpeed", "100", "125"),
        new DefaultChange(66, "particles.shootingSparks.size", "75", "85"),
        new DefaultChange(66, "particles.shootingSparks.gravity", "25", "15"),
        new DefaultChange(66, "particles.shootingSparks.velocityLoss", "25", "24"),
        new DefaultChange(66, "particles.shootingSparks.headFade", "12", "0"),
        new DefaultChange(66, "particles.shootingSparks.trailLifetime", "4", "12"),
        new DefaultChange(66, "particles.shootingSparks.trailFade", "12", "0"),
        new DefaultChange(66, "particles.shootingSparks.squaresPerTick", "24", "48"),
        new DefaultChange(64, "particles.shootingSparks.intensity", "18", "8"),
        new DefaultChange(64, "particles.shootingSparks.size", "100", "75"),
        new DefaultChange(64, "particles.shootingSparks.gravity", "15", "25"),
        new DefaultChange(64, "particles.shootingSparks.velocityLoss", "3", "25"),
        new DefaultChange(64, "particles.shootingSparks.headFade", "4", "12"),
        new DefaultChange(64, "particles.shootingSparks.trailLifetime", "12", "4"),
        new DefaultChange(64, "particles.shootingSparks.trailOpacity", "65", "100"),
        new DefaultChange(64, "particles.shootingSparks.trailFade", "4", "12"),
        new DefaultChange(55, "particles.flyingSparks.baseIntensity", "8", "12"),
        new DefaultChange(55, "particles.dotStreaks.baseIntensity", "8", "12"),
        new DefaultChange(55, "particles.dotStreaks.perfectIntensity", "16", "8"),
        new DefaultChange(55, "particles.dotStreaks.damageScaling", "0", "4"),
        new DefaultChange(4, "display.impactFrames", "3", "1"),
        new DefaultChange(4, "display.hitlagFrames", "0", "8"),
        new DefaultChange(5, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(6, "sources.fallPerfectParry", "true", "false"),
        new DefaultChange(6, "sources.knockbackStrength", "150", "120"),
        new DefaultChange(6, "items.allowAnyItem", "false", "true"),
        new DefaultChange(6, "shields.weaponPushback", "35", "80"),
        new DefaultChange(6, "display.screenFlash", "false", "true"),
        new DefaultChange(6, "display.flashStrength", "0", "100"),
        new DefaultChange(6, "display.hitlagFrames", "8", "6"),
        new DefaultChange(8, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(8, "display.impactBrightness", "100", "186"),
        new DefaultChange(8, "display.impactContrast", "100", "110"),
        new DefaultChange(8, "display.impactEdges", "100", "200"),
        new DefaultChange(8, "display.impactGrain", "100", "101"),
        new DefaultChange(11, "defense.retaliationCap", null, "25"),
        new DefaultChange(11, "shields.maxBlocks", "3", "5"),
        new DefaultChange(11, "shields.perfectDamage", "10", "0"),
        new DefaultChange(11, "shields.rechargeTicks", "30", "20"),
        new DefaultChange(11, "sources.fallPerfectParry", "true", "false"),
        new DefaultChange(11, "display.shakeStrength", "60", "100"),
        new DefaultChange(12, "timing.parryTicks", "6", "4"),
        new DefaultChange(12, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(12, "sources.knockbackStrength", "120", "80"),
        new DefaultChange(12, "shields.parryTicks", "8", "6"),
        new DefaultChange(12, "shields.perfectDamage", "0", "10"),
        new DefaultChange(17, "mobs.guardDifficulty", "40% periodic stance chance at maximum", "40% initial chance after landed hits, increasing with pressure"),
        new DefaultChange(18, "mobs.blocking", "true", "false"),
        new DefaultChange(18, "mobs.swordChance", "10%", "100%"),
        new DefaultChange(18, "mobs.axeChance", "10%", "100%"),
        new DefaultChange(18, "mobs.shieldChance", "10%", "100%"),
        new DefaultChange(18, "mobs.swordShieldChance", "10%", "100%"),
        new DefaultChange(18, "mobs.axeShieldChance", "10%", "100%"),
        new DefaultChange(21, "mobs.hitsBeforeGuard", "2", "1"),
        new DefaultChange(21, "mobs.tacticalChance", "15", "35"),
        new DefaultChange(21, "mobs.tacticalMaxSeconds", "10", "5"),
        new DefaultChange(22, "mobs.enabled", "true", "false"),
        new DefaultChange(22, "stance.damageRules", "empty", "minecraft:generic_kill=1"),
        new DefaultChange(22, "timing.parryTicks", "4", "6"),
        new DefaultChange(22, "mobs.perfectTicks", "8", "20"),
        new DefaultChange(22, "mobs.parryTicks", "20", "60"),
        new DefaultChange(22, "mobs.approachChance", "40", "100"),
        new DefaultChange(22, "mobs.tacticalChance", "35", "100"),
        new DefaultChange(22, "mobs.retaliationEnabled", "false", "true"),
        new DefaultChange(22, "mobs.guardMovement", "90", "100"),
        new DefaultChange(22, "mobs.approachDistance", "3", "2"),
        new DefaultChange(22, "mobs.approachMaxSeconds", "3", "2"),
        new DefaultChange(22, "mobs.rushChance", "60", "70"),
        new DefaultChange(22, "mobs.gearChance", "40", "50"),
        new DefaultChange(22, "display.impactBrightness", "186", "400"),
        new DefaultChange(22, "display.impactContrast", "110", "200"),
        new DefaultChange(22, "display.impactEdges", "200", "400"),
        new DefaultChange(22, "display.impactGrain", "101", "400"),
        new DefaultChange(22, "display.impactChromatic", "30", "400"),
        new DefaultChange(22, "display.chromaticIntensity", "35", "200"),
        new DefaultChange(22, "display.chromaticTicks", "8", "10"),
        new DefaultChange(22, "display.chromaticHud", "false", "true"),
        new DefaultChange(24, "stance.damageRules", "minecraft:generic_kill=1", "minecraft:generic_kill=1,minecraft:arrow=1,minecraft:lava=0,minecraft:hot_floor=0"),
                new DefaultChange(24, "items.allowAnyItem", "true", "false"),
        new DefaultChange(24, "shields.perfectTicks", "2", "1"),
        new DefaultChange(24, "shields.rechargeTicks", "20", "10"),
        new DefaultChange(24, "animation.firstPersonGuardPose", "true", "false"),
        new DefaultChange(28, "items.allowUsableItems", "true", "false"),
        new DefaultChange(29, "pose1", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose2", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose3", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose4", "300 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose5", "200/180 ms transitions", "250 ms transitions"),
        new DefaultChange(33, "pose1", "250 ms transitions", "original pose; 200 ms transitions"),
        new DefaultChange(33, "pose2", "250 ms transitions", "200 ms transitions"),
        new DefaultChange(33, "pose3", "250 ms transitions", "200 ms transitions"),
        new DefaultChange(33, "pose4", "250 ms transitions", "300 ms transitions"),
        new DefaultChange(33, "pose5", "enabled; 250 ms transitions", "disabled; 200/180 ms transitions"),
        new DefaultChange(34, "pose1–pose5", "previous preset defaults", "new preset poses; 180/240 ms transitions; Preset 5 enabled and empty-hand only"),
        new DefaultChange(36, "shields.shieldParryPriority", "1 (Shield)", "2 (Default: player main hand)"),
        new DefaultChange(37, "pose1", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose2", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose3", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose4", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose5", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(42, "timing.perfectTicks", "2", "3"),
        new DefaultChange(42, "timing.parryTicks", "6", "8"),
        new DefaultChange(42, "animation.firstPersonGuardPose", "false", "true")
    );

    static List<String> defaultChanges(int oldRevision, Set<String> currentPaths) {
        var changes = new ArrayList<String>();
        // A setting changed by several skipped revisions is reported once, from its oldest previous
        // default to its newest; changes that return to the original default are omitted.
        var net = new java.util.LinkedHashMap<String, String[]>();
        DEFAULT_CHANGES.stream()
            .filter(change -> change.revision() > oldRevision && change.revision() <= GuardConfig.CONFIG_REVISION
                && currentPaths.contains(change.path()))
            .sorted(java.util.Comparator.comparingInt(DefaultChange::revision))
            .forEach(change -> net.merge(change.path(), new String[] {change.previous(), change.current()},
                (oldest, newest) -> new String[] {oldest[0], newest[1]}));
        net.forEach((path, values) -> {
            if (!values[0].equals(values[1]))
                changes.add("Default changed: " + label(path) + " (" + values[0] + " -> " + values[1] + ")");
        });
        if (oldRevision < 103 && currentPaths.contains("parried.reaction"))
            changes.add("Updated: Parried Reaction adds Both (Guard Break and Stun).");
        return changes;
    }
    static int revisionIn(byte[] bytes) {
        String section = "";
        for (String line : new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) section = trimmed.substring(1, trimmed.length() - 1);
            else if (section.isEmpty() && trimmed.matches("configRevision\\s*=.*")) {
                try { return Integer.parseInt(trimmed.substring(trimmed.indexOf('=') + 1).trim()); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return -1; // Legacy file with no config revision.
    }

    static Set<String> pathsIn(byte[] bytes) {
        Set<String> paths = new LinkedHashSet<>();
        String section = "";
        for (String line : new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) section = trimmed.substring(1, trimmed.length() - 1) + ".";
            else if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                int equals = trimmed.indexOf('=');
                if (equals > 0) paths.add(section + trimmed.substring(0, equals).trim());
            }
        }
        return paths;
    }

    static String label(String path) {
        String spaced = path.replace('.', ' ').replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    static void removeStale(Config config, String prefix, Set<String> paths) {
        for (var entry : new ArrayList<>(config.entrySet())) {
            String key = entry.getKey();
            String path = prefix + key;
            Object value = entry.getValue();
            if (value instanceof Config group) {
                removeStale(group, path + ".", paths);
                if (group.isEmpty()) config.remove(key);
            } else if (!paths.contains(path)) config.remove(key);
        }
    }

    static void migrateMobSettings(byte[] original) {
        var old=new com.electronwill.nightconfig.toml.TomlParser().parse(new String(original,java.nio.charset.StandardCharsets.UTF_8));
        Object retaliation=old.get("mobs.retaliationEnabled");
        if(retaliation instanceof Boolean enabled){MOB_REGULAR_RETALIATION.set(enabled);MOB_PERFECT_RETALIATION.set(enabled);}
        if(old.get("mobs.whitelist")==null){
            var ids=new java.util.LinkedHashSet<String>(java.util.Arrays.asList(MOB_WHITELIST.getDefault().split(",")));
            Object extra=old.get("mobs.humanoidIds");
            if(extra instanceof String text)for(String id:text.split(","))if(!id.isBlank())ids.add(id.trim());
            Object blacklist=old.get("mobs.blacklist");
            if(blacklist instanceof String text)for(String id:text.split(","))ids.remove(id.trim());
            MOB_WHITELIST.set(String.join(",",ids));
        }
    }
}
