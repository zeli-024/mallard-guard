package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Connection-scoped enforcement, personal settings and cached cosmetic palettes. */
final class GuardClientPolicy {
    private GuardClientPolicy() {}
    private static boolean policyKnown, policyEnabled, policyRequested;
    private static int dontEnforceMask;
    private static int[] policyDefaults = GuardClientPreset.DEFAULTS.clone(), personalDefaults;
    public static boolean clientCategoryLocked(int category) {
        return category!=0 && policyEnabled && (dontEnforceMask & category) == 0;
    }

    private static String policyAnimations="";
    private static java.util.List<dev.zeli.mallardguard.GuardPoseLibrary.Pose> enforcedAnimations=java.util.List.of();
    static java.util.List<dev.zeli.mallardguard.GuardPoseLibrary.Pose> enforcedAnimations(){return enforcedAnimations;}
    public static GuardPackets.ClientPolicy currentPolicy() {
        return new GuardPackets.ClientPolicy(policyEnabled, dontEnforceMask, GuardClientPreset.encode(policyDefaults),policyAnimations);
    }

    // Immutable entries are captured when a burst arrives, before any hitlag delay.
    private static final java.util.Map<Integer, GuardPackets.Palette> playerPalettes = new java.util.HashMap<>();
    private static GuardPackets.Palette sentPalette;
    public static void playerPalette(GuardPackets.PlayerPalette data) {
        if (data.id() <= 0) return;
        if (data.colors() == null) playerPalettes.remove(data.id());
        else if (data.colors().valid()) playerPalettes.put(data.id(), data.colors());
    }
    static void shareParticlePalette() {
        if (Minecraft.getInstance().getConnection() == null || !policyKnown) return;
        GuardPackets.Palette colors = clientCategoryLocked(GuardClientPreset.PARTICLES) && personalDefaults != null
            ? GuardPackets.Palette.fromPreset(personalDefaults)
            : GuardPackets.Palette.readLocal();
        if (!colors.equals(sentPalette)) {
            PacketDistributor.sendToServer(new GuardPackets.SetPalette(colors));
            sentPalette = colors;
        }
    }

    public static void clientPolicy(GuardPackets.ClientPolicy data) {
        GuardDiagnostics.event(GuardDiagnostics.CONFIG,"Client policy received: enabled="+data.enabled()+", exceptions="+data.dontEnforceMask());
        var parsedAnimations=dev.zeli.mallardguard.GuardPoseLibrary.parseAnimations(data.animations());
        if(!data.animations().isEmpty()&&parsedAnimations==null)return;
        int[] values = GuardClientPreset.parse(data.defaults());
        if (values == null || data.dontEnforceMask() < 0 || (data.dontEnforceMask() & ~GuardClientPreset.ALL_CATEGORIES) != 0) return;
        policyAnimations=data.animations();enforcedAnimations=parsedAnimations==null?java.util.List.of():parsedAnimations;
        if (!policyKnown) {
            personalDefaults = GuardClientPreset.readLocal();
            policyKnown = true;
        }
        boolean firstActivation = data.enabled() && !policyEnabled;
        if (!data.enabled() && policyEnabled) {
            for (int category : GuardClientPreset.CATEGORIES) GuardClientPreset.apply(personalDefaults, category);
        }
        policyEnabled = data.enabled();
        dontEnforceMask = data.dontEnforceMask();
        policyDefaults = values;
        if (policyEnabled) {
            for (int category : GuardClientPreset.CATEGORIES) {
                if (firstActivation || clientCategoryLocked(category)) GuardClientPreset.apply(values, category);
            }
            if (firstActivation) {
                for (int index = 0; index < values.length; index++) {
                    if ((dontEnforceMask & GuardClientPreset.categoryOf(index)) != 0)
                        personalDefaults[index] = values[index];
                }
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GuardConfigScreen config) config.refreshPolicy();
        shareParticlePalette();
    }

    public static void saveClientConfig() {
        if (policyEnabled && personalDefaults != null) {
            int[] before=personalDefaults.clone();
            try{
                int[] edited=GuardClientPreset.readLocal();
                for(int index=0;index<edited.length;index++)if(!clientCategoryLocked(GuardClientPreset.categoryOf(index)))personalDefaults[index]=edited[index];
                for(int category:GuardClientPreset.CATEGORIES)if(clientCategoryLocked(category))GuardClientPreset.apply(personalDefaults,category);
                GuardConfig.CLIENT_SPEC.save();GuardConfig.PUNCHY_SPEC.save();
            }catch(RuntimeException error){personalDefaults=before;throw error;}
            finally{for(int category:GuardClientPreset.CATEGORIES)if(clientCategoryLocked(category))GuardClientPreset.apply(policyDefaults,category);}
        } else { GuardConfig.CLIENT_SPEC.save(); GuardConfig.PUNCHY_SPEC.save(); }
        GuardConfig.clearConfigBackups();
        shareParticlePalette();
    }

    static void clearClientPolicy() {
        if (policyKnown && policyEnabled && personalDefaults != null)
            for (int category : GuardClientPreset.CATEGORIES) GuardClientPreset.apply(personalDefaults, category);
        policyAnimations="";enforcedAnimations=java.util.List.of();
        policyKnown = policyEnabled = false;
        policyRequested = false;
        dontEnforceMask = 0;
        personalDefaults = null;
        policyDefaults = GuardClientPreset.DEFAULTS.clone();
        playerPalettes.clear(); sentPalette = null;
    }

    static GuardPackets.Palette palette(int id) { return playerPalettes.get(id); }
    static void requestIfNeeded() {
        if (policyRequested) return;
        policyRequested = true;
        PacketDistributor.sendToServer(new GuardPackets.RequestClientPolicy());
    }
}
