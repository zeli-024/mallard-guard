package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardItemRules;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

/** Search and category filters for the shared item assignment grid. */
final class GuardItemSearch {
    static final String[] CATEGORIES={"Blocks","Tools","Consumables","Shields","Other"};
    private GuardItemSearch(){}
    /** Screen-owned metadata; no static ItemStack or tooltip collection survives closing. */
    static final class Entry {
        final Item item;
        final String id,namespace;
        private String name;
        Entry(Item item){this.item=item;var key=BuiltInRegistries.ITEM.getKey(item);id=key.toString();namespace=key.getNamespace();}
        String name(){if(name==null)name=item.getDescription().getString().toLowerCase(Locale.ROOT);return name;}
        boolean matches(String q,String tooltip){
            if(q.isEmpty())return true;
            if(q.startsWith("@"))return namespace.contains(q.substring(1));
            // Keep tag membership live so datapack reloads do not leave stale eligibility.
            if(q.startsWith("#"))return item.getDefaultInstance().getTags().anyMatch(tag->tag.location().toString().contains(q.substring(1)));
            if(q.contains(":"))return id.equals(q);
            return id.contains(q)||name().contains(q)||tooltip.contains(q)||item.getDefaultInstance().getTags().anyMatch(tag->tag.location().toString().contains(q));
        }
    }
    static int categories(Item item){
        var stack=item.getDefaultInstance();int mask=0;
        if(item instanceof BlockItem)mask|=1;
        if(GuardItemRules.hasAttackDamage(stack))mask|=2;
        if(GuardItemRules.consumable(stack))mask|=4;
        if(GuardClient.shieldLike(stack))mask|=8;
        return mask==0?16:mask;
    }
}
