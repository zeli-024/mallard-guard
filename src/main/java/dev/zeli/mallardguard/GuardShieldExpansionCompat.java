package dev.zeli.mallardguard;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional Shield Expansion stats; Mallard Guard remains the sole combat handler. */
public final class GuardShieldExpansionCompat {
    private static boolean checked,active;
    private static Field stats,advancedExplosions;
    private GuardShieldExpansionCompat() {}
    static synchronized boolean initialize(){
        if(checked)return active;
        checked=true;
        if(!ModList.get().isLoaded("shieldexp"))return false;
        try{
            advancedExplosions=Class.forName("org.infernalstudios.shieldexp.config.ShieldExpansionConfig").getField("ADVANCED_EXPLOSIONS");
            stats=Class.forName("org.infernalstudios.shieldexp.init.ShieldDataLoader").getField("SHIELD_STATS");
            active=true;
        }catch(ReflectiveOperationException|LinkageError error){report(error);}
        return active;
    }
    static void disable(){active=false;}
    private static void report(Throwable error){org.slf4j.LoggerFactory.getLogger(MallardGuard.ID).warn("Shield Expansion stats are unavailable; using ordinary Mallard Guard settings.",error);}
    public static boolean active(){return active;}
    private static double value(ItemStack stack,String key,double fallback){
        if(!active || stack.isEmpty())return fallback;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("shieldexp"))return fallback;
        try{
            // Read the current map so datapack reloads cannot leave stale stats behind.
            Object table=stats.get(null);
            if(table instanceof Map<?,?> all && all.get(id.toString()) instanceof Map<?,?> entry
                && entry.get(key) instanceof Number n && Double.isFinite(n.doubleValue()))return n.doubleValue();
        }catch(IllegalAccessException error){active=false;report(error);}
        return fallback;
    }
    public static int blockCount(ItemStack stack){double stamina=value(stack,"stamina",-1);return stamina<0?-1:(int)Math.clamp(stamina+1,1,GuardItemBlockCounts.MAX_COUNT);}
    public static int parryTicks(ItemStack stack,int fallback){return (int)Math.clamp(value(stack,"parryTicks",fallback),1,10);}
    public static int cooldownTicks(ItemStack stack,int fallback){return (int)Math.clamp(value(stack,"cooldownTicks",fallback),1,200);}
    public static double movement(ItemStack stack,double fallback){double factor=value(stack,"speedFactor",-1);return factor<0?fallback:Math.clamp(0.2+0.8*factor,0,1);}
    public static double explosionBlockedFraction(ItemStack stack){
        try{if(!active || !advancedExplosions.getBoolean(null))return 1;}catch(IllegalAccessException error){active=false;report(error);return 1;}
        double resistance=value(stack,"blastResistance",-1);return resistance<0?1:Math.clamp((1+resistance)/2,0,1);}
}
