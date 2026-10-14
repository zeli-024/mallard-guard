package dev.zeli.mallardguard.mixin;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.neoforged.fml.loading.LoadingModList;
public final class GuardMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("EmfGuardPoseMixin")) return LoadingModList.get().getModFileById("entity_model_features") != null;
        if (mixin.endsWith("BetterCombatGuardMixin")) return LoadingModList.get().getModFileById("bettercombat") != null;
        if (mixin.contains("Punchy")) {
            var file = LoadingModList.get().getModFileById("punchy");
            return file != null && dev.zeli.mallardguard.GuardPunchyApi.compatible()
                && (!mixin.endsWith("PunchyDiagnosticMixin") || dev.zeli.mallardguard.GuardPunchyApi.diagnosticsCompatible());
        }
        return true;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
