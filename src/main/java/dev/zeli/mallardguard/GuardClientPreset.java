package dev.zeli.mallardguard;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import java.util.stream.Collectors;

/** Values used when a server supplies initial or enforced client settings. */
public final class GuardClientPreset {
    public static final int PARTICLES = 1, HUD = 2, AUDIO = 4, SCREEN=8, ANIMATIONS=16, PUNCHY = 32;
    public static final int[] CATEGORIES = {PARTICLES, HUD, AUDIO, SCREEN, ANIMATIONS, PUNCHY};
    public static final int ALL_CATEGORIES = 63;
    public static final int[] DEFAULTS = GuardScreenSettings.append(GuardPoseSettings.appendMotion(GuardShieldReactions.append(GuardPoseSettings.appendDefaults(GuardClientSettings.defaults()),false),false),false);
    static final int[] MIN = GuardClientSettings.minimums();
    static final int[] MAX = GuardClientSettings.maximums();

    // The copied client config is an optional, editable template on the server.
    private static final String[] TEMPLATE_KEYS = GuardClientSettings.keys();

    public static Path enforceTemplatePath() {
        return FMLPaths.CONFIGDIR.get().resolve("mallard_guard/enforce/client.toml");
    }

    private static boolean booleanField(int index) { return GuardClientSettings.flag(index); }

    public static int[] readEnforceTemplate(int[] fallback) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return fallback;
        int[] values = fallback.clone();
        GuardConfig.preserveServerFile(path);
        GuardConfig.preserveServerFile(path.getParent().resolve("mg_punchy/config.toml"));
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] == null) continue;
                Object raw = config.get(TEMPLATE_KEYS[i]);
                if (raw instanceof Boolean booleanValue && booleanField(i)) values[i] = booleanValue ? 1 : 0;
                else if (raw instanceof Number number && !booleanField(i) && Double.isFinite(number.doubleValue())) {
                    int value = (int)Math.round(number.doubleValue()*GuardClientSettings.scale(i));
                    if (value >= MIN[i] && value <= MAX[i]) values[i] = value;
                }
            }
            GuardShieldReactions.readTemplate(config,values);
            GuardSparkTracerConfig.read(config,values);
            GuardParticleColors.read(config,values);GuardScreenSettings.read(config,values);
            Number particleRevision=config.get("configRevision");if(particleRevision==null||particleRevision.intValue()<70){
                GuardParticleConfig.migratePerfect(values);
                if(config.get("particles.flyingSparks.perfectIntensity")==null)values[17]=DEFAULTS[17];
                if(config.get("particles.dotStreaks.perfectIntensity")==null)values[20]=DEFAULTS[20];
                if(config.get("particles.shootingSparks.perfectIntensity")==null && config.get("particles.shootingSparks.intensity")==null)values[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY]=DEFAULTS[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY];
            }
            Object punchy = config.get("punchy.enabled");
            if (punchy instanceof Boolean enabled) values[65] = enabled ? 1 : 0;
            for (int i = 0; i < GuardPoseSettings.COUNT; i++) {
                Object raw = config.get("punchy.pose" + (i + 1));
                int[] pose = raw instanceof String text ? GuardPoseSettings.parsePose(text) : null;
                if (pose != null) System.arraycopy(pose, 0, values, GuardPoseSettings.OFFSET + i * GuardPoseSettings.STRIDE, GuardPoseSettings.STRIDE);
            }
            Path punchyPath = path.getParent().resolve("mg_punchy/config.toml");
            if(Files.isRegularFile(punchyPath)) try(CommentedFileConfig profile=CommentedFileConfig.builder(punchyPath).sync().build()) {
                profile.load();Object delay=profile.get("releaseDelayMillis");if(delay instanceof Number n&&n.intValue()>=0&&n.intValue()<=1000)values[GuardPoseSettings.RELEASE_DELAY_SLOT]=n.intValue(); Object enabled=profile.get("enabled");if(enabled instanceof Boolean b)values[65]=b?1:0;
                for(int i=0;i < GuardPoseSettings.COUNT;i++){Object raw=profile.get("pose"+(i+1));int[] pose=raw instanceof String text?GuardPoseSettings.parsePose(text):null;if(pose!=null)System.arraycopy(pose,0,values,GuardPoseSettings.OFFSET+i*23,23);}
                // Legacy shared motion is only a fallback for presets without their own saved values.
                String[] fields={"raiseMillis","lowerMillis","curve","strength"};
                for(int i=0;i < GuardPoseSettings.COUNT;i++) {
                    Object pose=profile.get("pose"+(i+1));
                    if(pose instanceof String text&&GuardPoseSettings.parsePose(text)!=null)continue;
                    for(int f=0;f<fields.length;f++) {
                        Object raw=profile.get(fields[f]);
                        if(raw instanceof Number n&&n.intValue()>=GuardPoseSettings.min(19+f)&&n.intValue()<=GuardPoseSettings.max(19+f))
                            values[GuardPoseSettings.OFFSET+i*GuardPoseSettings.STRIDE+19+f]=n.intValue();
                    }
                }
            }
            Number revision = config.get("configRevision");
            if (revision == null || revision.intValue() < 12) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (!GuardParticleConfig.slot(i) && TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = GuardPresetMigration.clampLegacy(i, values[i]);
                }
            }
            if (revision == null || revision.intValue() < 15) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (!GuardParticleConfig.slot(i) && TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = GuardPresetMigration.clampLegacy(i, values[i]);
                }
            }

            return values;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not load enforce template: " + error.getMessage());
            return fallback;
        }
    }

    public static boolean writeEnforceTemplate(int[] values) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return true;
        GuardConfig.preserveServerFile(path);
        GuardConfig.preserveServerFile(path.getParent().resolve("mg_punchy/config.toml"));
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] != null) {
                    Object value;
                    if(booleanField(i))value=Boolean.valueOf(values[i]!=0);
                    else if(GuardClientSettings.scale(i)!=1)value=Double.valueOf(values[i]/(double)GuardClientSettings.scale(i));
                    else value=Integer.valueOf(values[i]);
                    config.set(TEMPLATE_KEYS[i], value);
                }
            }
            GuardShieldReactions.writeTemplate(config,values);
            GuardParticleConfig.writeTemplate(config,values);
            config.remove("display.offhandGuardPriority");
            config.remove("display.perfectOnlyHitlag");
            config.remove("punchy");
            Path directory=path.getParent().resolve("mg_punchy");
            try{Files.createDirectories(directory);}catch(java.io.IOException error){throw new IllegalStateException(error);}
            try(CommentedFileConfig profile=CommentedFileConfig.builder(directory.resolve("config.toml")).sync().build()) {
                profile.load();profile.remove("reaction");profile.set("releaseDelayMillis",values[GuardPoseSettings.RELEASE_DELAY_SLOT]);profile.set("configRevision",GuardConfig.CONFIG_REVISION);profile.set("enabled",values[65]!=0);for(int i=0;i < GuardPoseSettings.COUNT;i++){profile.set("pose"+(i+1),GuardPoseSettings.encodePose(GuardPoseSettings.pose(values,i)));}
                for(String legacy:new String[]{"raiseMillis","lowerMillis","curve","strength"})profile.remove(legacy);
                profile.save();
            }
            GuardScreenSettings.write(config,values);
            config.set("configRevision", GuardConfig.CONFIG_REVISION);
            config.save();
            GuardConfig.clearBackups(path.getParent());
            return true;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not save enforce template: " + error.getMessage());
            return false;
        }
    }

    public static String settingName(int index) {
        if(GuardScreenSettings.slot(index))return GuardScreenSettings.key(index);
        if(GuardParticleColors.retired(index))return "Retired particle setting";
        if(GuardParticleColors.slot(index))return GuardParticleColors.key(index);
        if(GuardSparkTracerConfig.retired(index))return "Retired particle setting";
        if(GuardSparkTracerConfig.slot(index))return GuardSparkTracerConfig.key(index);
        if(GuardParticleConfig.legacySlot(index))return "Retired particle setting";
        if(index==GuardPoseSettings.RELEASE_DELAY_SLOT)return "Punchy release delay";
        if(index>=GuardShieldReactions.OFFSET)return GuardShieldReactions.key(index-GuardShieldReactions.OFFSET);
        if(index>=GuardPoseSettings.OFFSET){int local=index-GuardPoseSettings.OFFSET;return "Punchy preset "+(local/23+1)+" / value "+(local%23);}
        return index<TEMPLATE_KEYS.length&&TEMPLATE_KEYS[index]!=null?TEMPLATE_KEYS[index]:"Client setting "+index;
    }
    private GuardClientPreset() {}

    public static String encode(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    public static int[] parse(String text) { return GuardPresetMigration.parse(text); }

    public static boolean valid(String text) { return parse(text) != null; }

    public static int categoryOf(int index) {
        if(GuardScreenSettings.slot(index))return GuardScreenSettings.category(index);
        if(GuardParticleColors.slot(index) || GuardSparkTracerConfig.slot(index) || GuardParticleConfig.legacySlot(index))return PARTICLES;
        if(index==GuardPoseSettings.RELEASE_DELAY_SLOT)return PUNCHY;
        if (index >= GuardShieldReactions.OFFSET) return HUD;
        if (index >= 65) return PUNCHY;
        return GuardClientSettings.category(index);
    }
    public static int[] readLocal() {
        return GuardScreenSettings.append(GuardPoseSettings.appendMotion(GuardShieldReactions.append(GuardPoseSettings.appendLocal(GuardClientSettings.readLocal()), true),true),true);
    }
    public static void apply(int[] values, int category) {
        if (category == PUNCHY) { GuardPoseSettings.apply(values); }
        else {
            GuardClientSettings.apply(values, category);GuardScreenSettings.apply(values,category);
            if(category==PARTICLES){GuardSparkTracerConfig.apply(values);GuardParticleColors.apply(values);}
            if (category == HUD) GuardShieldReactions.apply(values);
        }
    }
}
