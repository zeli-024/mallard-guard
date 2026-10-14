package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;

public final class GuardItemRules {
    public static final int MAX_LENGTH=2_097_152;
    private GuardItemRules() {}

    private static final TagKey<Item> COMMON_SHIELDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/shields"));
    private static final TagKey<Item> FORGE_SHIELDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "shields"));
    private record Rule(ResourceLocation id, TagKey<Item> tag,String namespace,String keyword) {}
    // Bound the cache because item rules may also come from a remote server.
    private record Rules(java.util.Set<ResourceLocation> included,java.util.Set<ResourceLocation> excluded,java.util.List<Rule> includedGroups,java.util.List<Rule> excludedGroups) {}
    private static int cachedCharacters;
    private static final int CACHE_CHARACTERS=524_288;
    private static final java.util.Map<String, Rules> RULES = new java.util.LinkedHashMap<>(64, 0.75F, true);

    /** One compiled list owned by a policy or editor. Group membership stays live. */
    public static final class Compiled {
        private final Rules rules;
        private Compiled(Rules rules) { this.rules=rules; }
        public boolean matches(ItemStack stack) { return matchesCompiled(stack,rules); }
    }
    /** Keeps the current list until its owner replaces it; unrelated previews cannot evict it. */
    public static final class Source {
        private String text;
        private Compiled compiled;
        public synchronized Compiled get(String value) {
            if(!value.equals(text)){compiled=compile(value);text=value;}
            return compiled;
        }
        public synchronized void clear() { text=null;compiled=null; }
    }
    private static final Source SERVER_INCLUDED=new Source(),SERVER_EXCLUDED=new Source();
    private static final Source SERVER_SHIELDS=new Source(),SERVER_SHIELD_BLACKLIST=new Source();
    private static final Source CLIENT_SHIELDS=new Source(),CLIENT_SHIELD_BLACKLIST=new Source();
    public static boolean clientShieldLike(ItemStack stack,String whitelist,String blacklist) {
        return shieldLike(stack,CLIENT_SHIELDS.get(whitelist),CLIENT_SHIELD_BLACKLIST.get(blacklist));
    }
    public static void clearClientShieldRules(){CLIENT_SHIELDS.clear();CLIENT_SHIELD_BLACKLIST.clear();}
    public static boolean included(ItemStack stack) { return SERVER_INCLUDED.get(GuardConfig.INCLUDED_ITEMS.get()).matches(stack); }
    public static boolean excluded(ItemStack stack) { return SERVER_EXCLUDED.get(GuardConfig.EXCLUDED_ITEMS.get()).matches(stack); }
    public static void clearServerRules() { SERVER_INCLUDED.clear();SERVER_EXCLUDED.clear();SERVER_SHIELDS.clear();SERVER_SHIELD_BLACKLIST.clear(); }

    public static Compiled compile(String text) {
        var included=new java.util.HashSet<ResourceLocation>();var excluded=new java.util.HashSet<ResourceLocation>();
        var positive=new java.util.ArrayList<Rule>();var negative=new java.util.ArrayList<Rule>();
        for(String entry:text.split(",")){
            String value=entry.trim().toLowerCase(java.util.Locale.ROOT);boolean except=value.startsWith("!");if(except)value=value.substring(1);
            Rule rule;
            if(value.startsWith("@"))rule=new Rule(null,null,value.substring(1),null);
            else if(!value.startsWith("#")&&!value.contains(":")){if(value.isBlank())continue;rule=new Rule(null,null,null,value);}
            else{boolean tag=value.startsWith("#");ResourceLocation id=ResourceLocation.tryParse(tag?value.substring(1):value);if(id==null)continue;
                if(!tag){(except?excluded:included).add(id);continue;}rule=new Rule(id,TagKey.create(Registries.ITEM,id),null,null);}
            (except?negative:positive).add(rule);
        }
        return new Compiled(new Rules(java.util.Set.copyOf(included),java.util.Set.copyOf(excluded),java.util.List.copyOf(positive),java.util.List.copyOf(negative)));
    }
    // Transient callers still have a bounded cache; active policies hold their own compiled lists.
    private static synchronized Rules parsedRules(String text) {
        Rules cached=RULES.get(text);
        if(cached!=null)return cached;
        cached=compile(text).rules;
        while(!RULES.isEmpty()&&(RULES.size()>=64||cachedCharacters+text.length()>CACHE_CHARACTERS)){
            String oldest=RULES.keySet().iterator().next();cachedCharacters-=oldest.length();RULES.remove(oldest);
        }
        if(text.length()<=CACHE_CHARACTERS){cachedCharacters+=text.length();RULES.put(text,cached);}
        return cached;
    }

    public static boolean shieldLike(ItemStack stack) {
        return shieldLike(stack,SERVER_SHIELDS.get(GuardConfig.SHIELD_ITEMS.get()),SERVER_SHIELD_BLACKLIST.get(GuardConfig.SHIELD_BLACKLIST.get()));
    }
    public static boolean shieldLike(ItemStack stack,String whitelist,String blacklist) {
        return shieldLike(stack,new Compiled(parsedRules(whitelist)),new Compiled(parsedRules(blacklist)));
    }
    public static boolean shieldLike(ItemStack stack,Compiled whitelist,Compiled blacklist) {
        return !stack.isEmpty() && !blacklist.matches(stack) && (stack.is(Items.SHIELD)
            || stack.is(COMMON_SHIELDS)
            || stack.is(FORGE_SHIELDS)
            || whitelist.matches(stack)
            || stack.getUseAnimation() == UseAnim.BLOCK && !hasAttackDamage(stack));
    }

    public static boolean hasAttackDamage(ItemStack stack) {
        return hasAttackDamage(stack, true);
    }

    public static boolean hasAttackDamage(ItemStack stack, boolean acceptOffhandSlot) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow()) && entry.modifier().amount() > 0
                && (entry.slot().test(EquipmentSlot.MAINHAND) || acceptOffhandSlot && entry.slot().test(EquipmentSlot.OFFHAND))) return true;
        }
        return false;
    }



    public static boolean consumableInEitherHand(Player player) {
        return consumable(player.getMainHandItem()) || consumable(player.getOffhandItem());
    }

    public static boolean consumable(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return !stack.isEmpty() && (animation == UseAnim.EAT || animation == UseAnim.DRINK
            || stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    public static boolean matches(ItemStack stack, String rules) {
        return !rules.isBlank() && matchesCompiled(stack,parsedRules(rules));
    }
    private static boolean ruleMatches(ItemStack stack,ResourceLocation itemId,Rule rule){
        return rule.namespace()!=null?rule.namespace().equals(itemId.getNamespace())
            :rule.keyword()!=null?itemId.toString().contains(rule.keyword())||stack.getTags().anyMatch(tag->tag.location().toString().contains(rule.keyword())):stack.is(rule.tag());
    }
    private static boolean matchesCompiled(ItemStack stack,Rules parsed){
        ResourceLocation id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(parsed.excluded().contains(id))return false;
        // Exceptions always win; positive exact lookups avoid scanning unrelated positive groups.
        for(Rule rule:parsed.excludedGroups())if(ruleMatches(stack,id,rule))return false;
        if(parsed.included().contains(id))return true;
        for(Rule rule:parsed.includedGroups())if(ruleMatches(stack,id,rule))return true;
        return false;
    }

    public static boolean validEntityRules(String rules){
        if(rules==null||rules.length()>1024)return false;if(rules.isBlank())return true;
        for(String entry:rules.split(",",-1)){String value=entry.trim();if(value.isEmpty()||value.startsWith("@")||ResourceLocation.tryParse(value.startsWith("#")?value.substring(1):value)==null)return false;}
        return true;
    }

    public static boolean validEntityIds(String rules){
        if(rules==null||rules.length()>1024)return false;if(rules.isBlank())return true;
        for(String value:rules.split(",",-1))if(value.trim().isEmpty()||value.trim().startsWith("#")||value.trim().startsWith("@")||ResourceLocation.tryParse(value.trim())==null)return false;
        return true;
    }

    public static boolean valid(String rules) {
        if (rules == null || rules.length() > MAX_LENGTH) return false;
        if (rules.isBlank()) return true;
        for (String entry : rules.split(",", -1)) {
            String value = entry.trim();
            if(value.startsWith("!"))value=value.substring(1);
            if(value.isEmpty())return false;
            if(value.startsWith("@")){if(!value.substring(1).matches("[a-z0-9_.-]+"))return false;}
            else if(value.startsWith("#")||value.contains(":")){if(ResourceLocation.tryParse(value.startsWith("#")?value.substring(1):value)==null)return false;}
            else if(!value.matches("[a-z0-9_./-]+"))return false;
        }
        return true;
    }
}
