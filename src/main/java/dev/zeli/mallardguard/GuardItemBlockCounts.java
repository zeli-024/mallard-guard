package dev.zeli.mallardguard;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Exact item overrides, followed by compatibility values and ordinary guard limits. */
public final class GuardItemBlockCounts {
    public static final int MAX_COUNT = 100, MAX_LENGTH = GuardItemRules.MAX_LENGTH;
    /** Tracks the serialized budget without encoding the entire map on every typed count. */
    public static final class Draft extends java.util.AbstractMap<String,Integer> {
        private final LinkedHashMap<String,Integer> entries=new LinkedHashMap<>();
        private int characters;
        private static int width(String id,int count){return id.length()+1+Integer.toString(count).length();}
        @Override public Integer put(String id,Integer count){Integer old=entries.put(id,count);characters+=width(id,count)-(old==null?(size()>1?-1:0):width(id,old));return old;}
        @Override public Integer remove(Object id){Integer old=entries.remove(id);if(old!=null)characters-=width((String)id,old)+(isEmpty()?0:1);return old;}
        @Override public void clear(){entries.clear();characters=0;}
        @Override public void putAll(Map<? extends String,? extends Integer> values){values.forEach(this::put);}
        @Override public int size(){return entries.size();}
        @Override public Integer get(Object id){return entries.get(id);}
        @Override public boolean containsKey(Object id){return entries.containsKey(id);}
        @Override public java.util.Set<Map.Entry<String,Integer>> entrySet(){return java.util.Collections.unmodifiableMap(entries).entrySet();}
        public int encodedLength(){return characters;}
    }
    private static String cachedText;
    private static Map<String,Integer> cached = Map.of();
    private GuardItemBlockCounts() {}

    public static Map<String,Integer> parse(String text) {
        if(text==null || text.length()>MAX_LENGTH)throw new IllegalArgumentException("Block count list is too long.");
        Map<String,Integer> result=new LinkedHashMap<>();
        if(text.isBlank())return result;
        for(String entry:text.split(",",-1)){
            String[] pair=entry.trim().split("=",-1);
            if(pair.length!=2 || !pair[0].contains(":") || ResourceLocation.tryParse(pair[0])==null)
                throw new IllegalArgumentException("Use an exact item ID and a block count.");
            int count=Integer.parseInt(pair[1]);
            if(count<0 || count>MAX_COUNT || result.putIfAbsent(pair[0],count)!=null)
                throw new IllegalArgumentException("Counts must be unique and between 0 and 100.");
        }
        return result;
    }
    public static boolean valid(String text){try{parse(text);return true;}catch(IllegalArgumentException error){return false;}}
    public static String encode(Map<String,Integer> entries){return entries.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining(","));}
    private static synchronized Map<String,Integer> configured(String text){
        if(!text.equals(cachedText)){cached=Map.copyOf(parse(text));cachedText=text;}
        return cached;
    }
    public static int resolve(ItemStack stack,boolean shield,String assignments,int defaultCount){
        if(!stack.isEmpty()){
            Integer assigned=configured(assignments).get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if(assigned!=null)return assigned;
            if(shield){int compatibility=GuardShieldExpansionCompat.blockCount(stack);if(compatibility>=0)return compatibility;}
        }
        return defaultCount;
    }
    public static int limit(ItemStack stack,boolean shield){return resolve(stack,shield,GuardConfig.ITEM_BLOCK_COUNTS.get(),(shield?GuardConfig.SHIELD_MAX_BLOCKS:GuardConfig.TOOL_MAX_BLOCKS).get());}
}
