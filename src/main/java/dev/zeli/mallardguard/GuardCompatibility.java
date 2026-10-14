package dev.zeli.mallardguard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Automatic optional-mod setup and notices; runtime combat and animation stay with their handlers. */
public final class GuardCompatibility {
    private static boolean shieldChecked,punchyChecked;
    private static Field shieldItemOnly,shieldHud;
    private static Method shieldSave;
    private GuardCompatibility() {}

    public static boolean simplySwordsWeapon(ItemStack stack){
        return !stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("simplyswords");
    }
    public static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(GuardCompatibility::shieldExpansion);}
    public static void serverStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event){shieldExpansion();}
    private static synchronized void shieldExpansion(){
        if(!shieldChecked){
            shieldChecked=true;
            if(!ModList.get().isLoaded("shieldexp"))return;
            try{
                Class<?> config=Class.forName("org.infernalstudios.shieldexp.config.ShieldExpansionConfig");
                shieldItemOnly=config.getField("ITEM_ONLY_MODE");shieldHud=config.getField("HUD_INDICATOR");shieldSave=config.getMethod("save");
            }catch(ReflectiveOperationException|LinkageError error){shieldItemOnly=null;report("Shield Expansion setup",error);return;}
        }
        if(shieldItemOnly==null)return;
        try{
            if(!GuardShieldExpansionCompat.initialize())return;
            boolean changed=!shieldItemOnly.getBoolean(null)||shieldHud.getBoolean(null);
            shieldItemOnly.setBoolean(null,true);shieldHud.setBoolean(null,false);
            if(changed)shieldSave.invoke(null);
        }catch(ReflectiveOperationException|LinkageError error){GuardShieldExpansionCompat.disable();report("Shield Expansion setup",error);}
    }

    /** Called on the client thread after configs load. Saved markers keep notices and defaults one-time. */
    public static void clientReady(Consumer<Component> notice,boolean shieldExpansionActive){
        boolean changed=false;
        if(ModList.get().isLoaded("simplyswords")&&!GuardConfig.SIMPLY_SWORDS_NOTICE_SHOWN.get()){
            GuardConfig.SIMPLY_SWORDS_NOTICE_SHOWN.set(true);changed=true;
            notice.accept(Component.translatable("message.mallardguard.simply_swords_bind_ability"));
        }
        if(shieldExpansionActive&&ModList.get().isLoaded("shieldexp")&&!GuardConfig.SHIELD_EXPANSION_NOTICE_SHOWN.get()){
            GuardConfig.SHIELD_EXPANSION_NOTICE_SHOWN.set(true);changed=true;
            notice.accept(Component.translatable("message.mallardguard.shield_expansion_item_only"));
        }
        if(!punchyChecked){
            punchyChecked=true;
            if(ModList.get().isLoaded("punchy")&&!GuardConfig.PUNCHY_MOVEMENT_DEFAULTS_APPLIED.get()&&punchyMovementDefaults()){
                GuardConfig.PUNCHY_MOVEMENT_DEFAULTS_APPLIED.set(true);changed=true;
                notice.accept(Component.translatable("message.mallardguard.punchy_movement_defaults"));
            }
        }
        if(changed){GuardConfig.CLIENT_SPEC.save();GuardConfig.clearConfigBackups();}
    }
    private static boolean punchyMovementDefaults(){
        try{
            Class<?> config=Class.forName("punchy.config.PunchyConfig");
            Object data=config.getMethod("snapshot").invoke(null);
            Class<?> type=data.getClass();
            Field walk=type.getField("enableCustomWalk"),sprint=type.getField("enableSprintArmSwing");
            Method save=config.getMethod("applyAndSave",type);
            if(walk.getBoolean(data)||sprint.getBoolean(data)){
                walk.setBoolean(data,false);sprint.setBoolean(data,false);save.invoke(null,data);
            }
            return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){report("Punchy movement defaults",error);return false;}
    }
    private static void report(String action,Throwable error){org.slf4j.LoggerFactory.getLogger(MallardGuard.ID).warn("{} could not be applied; existing settings are retained.",action,error);}
}
