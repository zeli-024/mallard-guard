package dev.zeli.mallardguard;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import net.minecraft.world.item.ItemStack;

/** Shared assignment matching and editable rule lists. */
public final class GuardItemAssignments {
    private GuardItemAssignments() {}

    public static final class Rules {
        private final GuardItemRules.Compiled allowed, denied, exceptions;
        private final boolean specific;
        public Rules(Collection<String> include, Collection<String> exclude) {
            allowed=GuardItemRules.compile(String.join(",",include));
            denied=GuardItemRules.compile(String.join(",",exclude));
            exceptions=GuardItemRules.compile(include.stream().filter(s->s.startsWith("!")).map(s->s.substring(1)).collect(java.util.stream.Collectors.joining(",")));
            specific=include.stream().anyMatch(s->!s.startsWith("!"));
        }
        public boolean allows(ItemStack stack, boolean fallback) {
            return !stack.isEmpty() && !denied.matches(stack) && !exceptions.matches(stack)
                && (specific ? allowed.matches(stack) : fallback);
        }
    }

    /** Compiles only after edits; scrolling and rendering reuse the current matcher. */
    public static final class Draft extends LinkedHashSet<String> {
        private String encoded;
        private final GuardItemRules.Source source=new GuardItemRules.Source();
        public Draft(Collection<String> entries) { addAll(entries); }
        @Override public boolean add(String value) { boolean changed=super.add(value);if(changed)encoded=null;return changed; }
        @Override public boolean remove(Object value) { boolean changed=super.remove(value);if(changed)encoded=null;return changed; }
        @Override public void clear() { super.clear();encoded=null; }
        @Override public boolean addAll(Collection<? extends String> values) { boolean changed=false;for(String value:values)changed|=add(value);return changed; }
        @Override public Iterator<String> iterator() {
            Iterator<String> iterator=super.iterator();
            return new Iterator<>() {
                public boolean hasNext(){return iterator.hasNext();}
                public String next(){return iterator.next();}
                public void remove(){iterator.remove();encoded=null;}
            };
        }
        public String text() { if(encoded==null)encoded=String.join(",",this);return encoded; }
        public GuardItemRules.Compiled compiled() { return source.get(text()); }
    }
}
