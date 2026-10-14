package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPoseLibrary;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Reviews ownership on the client thread in small, cancellable steps. */
final class GuardPoseOwnershipScreen extends Screen {
    private final Screen parent;
    private final List<GuardPoseLibrary.Pose> proposed;
    private final Consumer<List<GuardPoseLibrary.Pose>> accept;
    private final GuardPoseLibrary.Pose owner;
    private final List<GuardPoseLibrary.Pose> others;
    private final Map<String,Set<String>> overlaps=new LinkedHashMap<>();
    private final Iterator<Item> items=BuiltInRegistries.ITEM.iterator();
    private ItemStack current;
    private int otherIndex,scanned;
    private boolean complete;
    private int left,top,panelWidth;
    static void review(Screen parent,List<GuardPoseLibrary.Pose> proposed,String ownerId,Consumer<List<GuardPoseLibrary.Pose>> accept){
        if(ownerId==null){finish(parent,proposed,accept);return;}
        Minecraft.getInstance().setScreen(new GuardPoseOwnershipScreen(parent,proposed,ownerId,accept));
    }
    private GuardPoseOwnershipScreen(Screen parent,List<GuardPoseLibrary.Pose> proposed,String ownerId,Consumer<List<GuardPoseLibrary.Pose>> accept){
        super(Component.literal("Review Preset Assignments"));this.parent=parent;this.proposed=proposed;this.accept=accept;
        owner=proposed.stream().filter(p->p.id.equals(ownerId)).findFirst().orElseThrow();owner.prepareRules();
        others=proposed.stream().filter(p->!p.id.equals(ownerId)&&!p.exclusiveItems.isEmpty()).toList();
        for(var pose:others)pose.prepareRules();
    }
    private static void finish(Screen parent,List<GuardPoseLibrary.Pose> proposed,Consumer<List<GuardPoseLibrary.Pose>> accept){
        if(GuardPoseLibrary.parseAnimations(GuardPoseLibrary.encodeAnimations(proposed))==null){
            GuardUi.choices(parent,"Assignments Could Not Be Saved","The edit exceeds the saved-data limits or contains invalid rules.\nYour previous assignments were kept.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(parent)));return;
        }
        accept.accept(proposed);
    }
    @Override protected void init(){
        panelWidth=GuardUiLayout.wideWidth(width);left=(width-panelWidth)/2;top=GuardUiLayout.screenTop(height);
        addRenderableWidget(GuardUi.button("Cancel",left+8,height-42,90,20,this::onClose));
    }
    @Override public void tick(){
        if(complete)return;
        long deadline=System.nanoTime()+2_000_000L;
        for(int steps=0;steps<256&&System.nanoTime()<deadline;steps++){
            if(owner.exclusiveItems.isEmpty()||others.isEmpty()){done();return;}
            if(current==null){
                if(!items.hasNext()){done();return;}
                var item=items.next();scanned++;current=item.getDefaultInstance();otherIndex=0;
                if(!owner.exclusive(current)){current=null;continue;}
            }
            var other=others.get(otherIndex++);
            if(other.exclusive(current))overlaps.computeIfAbsent(other.id,id->new LinkedHashSet<>()).add(BuiltInRegistries.ITEM.getKey(current.getItem()).toString());
            if(otherIndex==others.size())current=null;
        }
    }
    private void done(){
        complete=true;
        if(overlaps.isEmpty()){finish(parent,proposed,accept);return;}
        String names=others.stream().filter(p->overlaps.containsKey(p.id)).map(p->p.name).limit(5).collect(java.util.stream.Collectors.joining(", "));
        GuardUi.choices(parent,"Replace Exclusive Assignments?","Matching items are already owned by: "+names+".\n\nTransfer ownership to this preset?\nOther assignments remain unchanged.",new GuardUi.Choice("Replace Assignments",()->{
            for(var other:others){var ids=overlaps.get(other.id);if(ids==null)continue;var rules=new LinkedHashSet<>(other.exclusiveItems);for(String id:ids){rules.remove(id);rules.add("!"+id);}other.exclusiveItems=new ArrayList<>(rules);other.prepareRules();}
            finish(parent,proposed,accept);
        }),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(parent)));
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g,x,y,partial);GuardUi.panel(g,left,top,panelWidth,height-top-12,false);
        g.drawString(font,getTitle(),left+8,top+12,GuardUi.TEXT);
        g.drawString(font,"Checking exclusive assignments…",left+8,top+44,GuardUi.TEXT);
        g.drawString(font,scanned+" / "+BuiltInRegistries.ITEM.size()+" items",left+8,top+60,GuardUi.MUTED);
        super.render(g,x,y,partial);
    }
    @Override public void onClose(){complete=true;Minecraft.getInstance().setScreen(parent);}
}
