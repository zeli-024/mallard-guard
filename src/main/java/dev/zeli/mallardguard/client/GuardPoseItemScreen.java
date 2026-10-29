package dev.zeli.mallardguard.client;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import dev.zeli.mallardguard.GuardItemRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Registry-backed creative item picker. Empty assignments use server guard eligibility. */
final class GuardPoseItemScreen extends Screen {
    private final Screen parent;
    private final BiConsumer<List<String>,Integer> accept;
    private final dev.zeli.mallardguard.GuardItemAssignments.Draft selected;
    private int handMode;
    private final boolean showHandMode;
    private String filter="";
    private String defaultAllowed,defaultBlocked;
    private boolean ruleMode,combined,filtersOpen;
    private final dev.zeli.mallardguard.GuardItemAssignments.Draft blocked=new dev.zeli.mallardguard.GuardItemAssignments.Draft(List.of());
    private Set<String> savedBlocked=Set.of();
    private BiConsumer<String,String> acceptRules;
    private Button selectAllButton,clearResultsButton;
    private final Map<Item,List<Component>> itemTooltips=new LinkedHashMap<>(128,0.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Item,List<Component>> entry){return size()>128;}};
        private int tooltipCursor;
    private String normalizedQuery="";
    private final Map<Item,GuardItemSearch.Entry> metadata=new HashMap<>();
    private final Map<String,List<String>> namespaceItems=new LinkedHashMap<>();
    private BulkEdit bulkEdit;
    private static final long WORK_BUDGET_NS=2_000_000L;
    private abstract static class BulkEdit { int cursor,total; abstract boolean step(); abstract void finish(); }
    private boolean indexing;
    private long searchDeadline;
    private Consumer<PoseAssignments> poseAccept;
    private int tooltipCharacters;
    private static final int TOOLTIP_CHAR_BUDGET=4_194_304,TOOLTIP_ITEM_LIMIT=8192;
    private final Map<Item,String> tooltipSearch=new LinkedHashMap<>();
    private String ruleTitle="Assign items",notice="";
    private boolean countMode;
    private int assignmentCount=5;
    private EditBox countInput;
    private final dev.zeli.mallardguard.GuardItemBlockCounts.Draft counts=new dev.zeli.mallardguard.GuardItemBlockCounts.Draft();
    private String savedCounts="";
    private int categoryMask;
    private final Map<Item,Integer> categoryBits=new HashMap<>();
    private Button ruleButton;
    private List<String> assignedEntries=List.of();
    private final Map<String,Item> itemById=new HashMap<>();
    private EditBox search,ruleInput;
    private String ruleText="";
    private int dragBar;
    private double dragGrab;
    private int left,top,panelWidth,scroll,rows,columns,bottom,assignedLeft,assignedWidth,assignedScroll,assignedRows;
    private final List<Item> all=new ArrayList<>(),matches=new ArrayList<>();
    private final List<Button> rowRemovers=new ArrayList<>();
    private final List<EditBox> rowCounts=new ArrayList<>();
    private boolean bindingRows;
    private record State(List<String> items,int handMode){}
    private State saved;
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept){this(parent,items,handMode,accept,true);}
    GuardPoseItemScreen(Screen parent,List<String> items,int handMode,BiConsumer<List<String>,Integer> accept,boolean showHandMode){super(Component.literal("Assign items"));this.parent=parent;this.accept=accept;this.showHandMode=showHandMode;selected=new dev.zeli.mallardguard.GuardItemAssignments.Draft(items);this.handMode=handMode;saved=new State(List.copyOf(selected),handMode);for(Item i:BuiltInRegistries.ITEM)if(i!=Items.AIR){all.add(i);var entry=new GuardItemSearch.Entry(i);metadata.put(i,entry);itemById.put(entry.id,i);namespaceItems.computeIfAbsent(entry.namespace,ignored->new ArrayList<>()).add(entry.id);}filter();}

    GuardPoseItemScreen(Screen parent,String title,String rules,Consumer<String> accept){
        this(parent,rules.isBlank()?List.of():Arrays.stream(rules.split(",")).map(String::trim).toList(),0,(items,hand)->accept.accept(String.join(",",items)),false);
        ruleMode=true;ruleTitle=title;
    }
    GuardPoseItemScreen(Screen parent,String assignments,Consumer<String> accept){
        this(parent,dev.zeli.mallardguard.GuardItemBlockCounts.parse(assignments),accept);
    }
    private GuardPoseItemScreen(Screen parent,Map<String,Integer> assignments,Consumer<String> accept){
        this(parent,new ArrayList<>(assignments.keySet()),0,(items,hand)->{},false);
        countMode=ruleMode=true;ruleTitle="Item Block Counts";
        counts.putAll(assignments);savedCounts=countRules();countAccept=accept;
    }
    GuardPoseItemScreen(Screen parent,String title,String whitelist,String blacklist,BiConsumer<String,String> accept){
        this(parent,title,whitelist,value->{});combined=true;acceptRules=accept;
        if(!blacklist.isBlank())blocked.addAll(Arrays.stream(blacklist.split(",")).map(String::trim).toList());
        savedBlocked=Set.copyOf(blocked);
    }
    record PoseAssignments(List<String> allowed,List<String> denied,int handMode) {}
    GuardPoseItemScreen(Screen parent,String title,List<String> allowed,List<String> denied,int mode,boolean hands,Consumer<PoseAssignments> accept){
        this(parent,allowed,mode,(items,hand)->{},hands);ruleMode=combined=true;ruleTitle=title;blocked.addAll(denied);savedBlocked=Set.copyOf(blocked);
        poseAccept=accept;
    }
    GuardPoseItemScreen restrictItems(java.util.function.Predicate<Item> eligible){all.removeIf(item->!eligible.test(item));filter();return this;}
    GuardPoseItemScreen assignmentDefaults(String allowed,String denied){defaultAllowed=allowed;defaultBlocked=denied;return this;}
    private void resetAssignments(){
        if(bulkEdit!=null)return;
        selected.clear();blocked.clear();counts.clear();handMode=0;
        if(defaultAllowed!=null&&!defaultAllowed.isBlank())selected.addAll(Arrays.stream(defaultAllowed.split(",")).map(String::trim).toList());
        if(defaultBlocked!=null&&!defaultBlocked.isBlank())blocked.addAll(Arrays.stream(defaultBlocked.split(",")).map(String::trim).toList());
    }
    record Assignments(String whitelist,String blacklist,String blockCounts){}
    private Consumer<Assignments> acceptAssignments;
    GuardPoseItemScreen(Screen parent,String title,String whitelist,String blacklist,String blockCounts,Consumer<Assignments> accept){
        this(parent,title,whitelist,blacklist,(allowed,denied)->{});
        countMode=true;counts.putAll(dev.zeli.mallardguard.GuardItemBlockCounts.parse(blockCounts));
        savedCounts=countRules();acceptAssignments=accept;
    }
    private GuardUiLayout.Assignment layout;
    private Consumer<String> countAccept;
    private String countRules(){return dev.zeli.mallardguard.GuardItemBlockCounts.encode(counts);}
    private void setCount(String id,int count){
        if(bulkEdit!=null||count<0||count>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT)return;
        Integer old=counts.put(id,count);
        if(counts.encodedLength()>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_LENGTH){if(old==null)counts.remove(id);else counts.put(id,old);notice="Assignment list is full.";return;}
        boolean added=!combined&&selected.add(id);if(added)rebuild();
    }
    private int gridTop(){return layout.gridY(filtersOpen,ruleMode||showHandMode);}
    private String rules(){return selected.text();}
    private String queryRule(){return ruleText.trim().toLowerCase(Locale.ROOT);}
    private boolean validQuery(){String q=queryRule();return !q.isEmpty()&&!q.contains(",")&&GuardItemRules.valid(q);}
    private String ruleAction(){
        String q=queryRule();boolean remove=selected.contains(q);
        if(q.startsWith("@"))return remove?"Remove Mod Rule":"Assign Entire Mod";
        if(q.startsWith("#"))return remove?"Remove Tag Rule":"Assign Tag";
        if(q.contains(":"))return remove?"Remove Item":"Assign Item";
        return remove?"Remove Keyword":"Assign Keyword";
    }
    private int assignedTop(){return layout.assignedY(countMode||showHandMode&&combined);}
    private int maxAssignedScroll(){return Math.max(0,assignedEntries.size()-assignedRows);}
    private void toggleRule(){if(!validQuery())return;String q=queryRule();if(!selected.remove(q)){selected.add(q);if(rules().length()>GuardItemRules.MAX_LENGTH){selected.remove(q);notice="Assignment list is full. Remove unused rules.";}}rebuild();}
    private static String ruleLabel(String value){if(value.startsWith("!"))return "Exception: "+ruleLabel(value.substring(1));return value.startsWith("@")?"Mod: "+value.substring(1):value.startsWith("#")?"Tag: "+value.substring(1):value.contains(":")?"Item: "+value:"Keyword: "+value;}
    private int maxScroll(){return Math.max(0,(matches.size()+columns-1)/columns-rows);}
    private String entryRule(String value){return combined?value.substring(2):value;}
    private String assignmentLabel(String stored){
        String rule=entryRule(stored);if(!combined)return ruleLabel(rule);
        if(rule.startsWith("!"))return (blockedEntry(stored)?"Blacklist exception: ":"Whitelist exception: ")+rule.substring(1);
        return (stored.startsWith("= ")?"Block limit: ":blockedEntry(stored)?"Blocked: ":"Allowed: ")+ruleLabel(rule);
    }
    private boolean blockedEntry(String value){return combined&&value.startsWith("- ");}
    private void removeAssignment(String value){if(blockedEntry(value))blocked.remove(entryRule(value));else selected.remove(entryRule(value));counts.remove(entryRule(value));rebuild();}

    private boolean selected(Item item){return ruleMode&&(!countMode||combined)?selected.compiled().matches(item.getDefaultInstance()):selected.contains(BuiltInRegistries.ITEM.getKey(item).toString());}

    private void apply(boolean close){
        if(bulkEdit!=null){notice="Wait for the pending edit, or press Escape to cancel it.";return;}
        if(dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.ASSIGNMENTS))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.ASSIGNMENTS,"Assignment apply requested: allowRules="+selected.size()+", blockRules="+blocked.size()+", countMode="+countMode+", combined="+combined);
        if(combined){
            if(!GuardItemRules.valid(rules())||!GuardItemRules.valid(blocked.text())){notice="Assignment list is full. Remove unused rules.";return;}
            if(acceptAssignments!=null){acceptAssignments.accept(new Assignments(rules(),blocked.text(),countRules()));savedCounts=countRules();}
            else if(poseAccept==null)acceptRules.accept(rules(),blocked.text());savedBlocked=Set.copyOf(blocked);saved=new State(List.copyOf(selected),handMode);
            if(close)Minecraft.getInstance().setScreen(parent);else Minecraft.getInstance().setScreen(this);if(poseAccept!=null)poseAccept.accept(new PoseAssignments(List.copyOf(selected),List.copyOf(blocked),handMode));return;
        }
        if(countMode){countAccept.accept(countRules());savedCounts=countRules();saved=new State(List.copyOf(selected),handMode);if(close)onClose();else Minecraft.getInstance().setScreen(this);return;}
        if(ruleMode&&!GuardItemRules.valid(rules())){notice="Check the assignment rules.";return;}accept.accept(List.copyOf(selected),handMode);saved=new State(List.copyOf(selected),handMode);if(close)onClose();else Minecraft.getInstance().setScreen(this);}
    private List<Component> tooltip(Item item){return itemTooltips.computeIfAbsent(item,key->{try{return List.copyOf(Screen.getTooltipFromItem(Minecraft.getInstance(),key.getDefaultInstance()));}catch(RuntimeException failedTooltip){return List.of(key.getDescription(),Component.literal(BuiltInRegistries.ITEM.getKey(key).toString()));}});}
    private String searchableTooltip(Item item){
        String known=tooltipSearch.get(item);if(known!=null)return known;
        var text=new StringBuilder();for(Component line:tooltip(item)){if(text.length()>=TOOLTIP_ITEM_LIMIT)break;String plain=line.getString();int room=TOOLTIP_ITEM_LIMIT-text.length();text.append(plain,0,Math.min(room,plain.length()));if(text.length()<TOOLTIP_ITEM_LIMIT)text.append('\n');}
        String value=text.toString().toLowerCase(Locale.ROOT);
        while(!tooltipSearch.isEmpty()&&tooltipCharacters+value.length()>TOOLTIP_CHAR_BUDGET){var entries=tooltipSearch.entrySet().iterator();tooltipCharacters-=entries.next().getValue().length();entries.remove();}
        tooltipSearch.put(item,value);tooltipCharacters+=value.length();return value;
    }
    private boolean tooltipQuery(){String q=filter.trim();return !q.isEmpty()&&!q.startsWith("@")&&!q.startsWith("#")&&!q.contains(":");}
    private boolean categoryMatches(Item item){return categoryMask==0||(categoryBits.computeIfAbsent(item,GuardItemSearch::categories)&categoryMask)!=0;}
    private boolean match(Item item){return categoryMatches(item)&&metadata.get(item).matches(normalizedQuery,tooltipSearch.getOrDefault(item,""));}
    private void filter(){
        normalizedQuery=filter.trim().toLowerCase(Locale.ROOT);matches.clear();
        scroll=0;notice="";tooltipCursor=0;indexing=true;updateBulkButtons();
    }
    private boolean busy(){return indexing||searchDeadline!=0||bulkEdit!=null;}
    private void updateBulkButtons(){if(selectAllButton!=null){selectAllButton.active=!busy()&&!matches.isEmpty();if(clearResultsButton!=null)clearResultsButton.active=selectAllButton.active;}}
    @Override public void tick(){
        super.tick();long until=System.nanoTime()+WORK_BUDGET_NS;int processed=0;
        if(bulkEdit!=null){var job=bulkEdit;while(processed++<256&&System.nanoTime()<until){if(job.step()){bulkEdit=null;job.finish();updateBulkButtons();return;}}return;}
        if(searchDeadline!=0){if(System.nanoTime()<searchDeadline)return;searchDeadline=0;filter();}
        if(!indexing)return;
        while(tooltipCursor<all.size()&&processed++<(tooltipQuery()?256:2048)&&System.nanoTime()<until){Item item=all.get(tooltipCursor++);if(tooltipQuery())searchableTooltip(item);if(match(item)){matches.add(item);}}
        if(tooltipCursor>=all.size())indexing=false;updateBulkButtons();
    }
    private void startBulk(BulkEdit job){bulkEdit=job;notice="";updateBulkButtons();}
    private void updateRuleButton(){
        if(ruleButton!=null){ruleButton.active=validQuery();ruleButton.setMessage(Component.literal(combined?"Assign Rule":ruleAction()));}
    }
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        layout=GuardUiLayout.assignment(width,height);
        int available=layout.width();
        left=layout.left();top=layout.top();bottom=layout.bottom();
        panelWidth=layout.browserWidth();assignedLeft=layout.assignedLeft();assignedWidth=layout.assignedWidth();
        columns=Math.max(1,(panelWidth-26)/GuardUiLayout.ITEM_STEP);
        rows=Math.max(1,(layout.contentBottom()-gridTop())/GuardUiLayout.ITEM_STEP);
        assignedRows=Math.max(1,(layout.contentBottom()-assignedTop())/GuardUiLayout.ITEM_STEP);
        refreshEntries();
        scroll=Math.min(scroll,maxScroll());assignedScroll=Math.min(assignedScroll,maxAssignedScroll());
        ruleButton=null;countInput=null;ruleInput=null;
        search=addRenderableWidget(GuardUi.editBox(font,left+10,layout.searchY(),panelWidth-24,"Search items"));
        search.setMaxLength(256);search.setValue(filter);
        search.setResponder(text->{filter=text;searchDeadline=System.nanoTime()+200_000_000L;indexing=false;updateBulkButtons();});
        search.setHint(Component.literal("Name, tooltip, ID, #tag or @mod"));
        search.setTooltip(GuardUi.tooltip("Search the item browser.\nNames, IDs, tooltip text, #tags and @mods are supported.\nSearch never changes your assignments."));
        addRenderableWidget(GuardUi.button("?",assignedLeft+assignedWidth-24,top+3,20,20,this::help));
        int tabWidth=layout.tabWidth(),tabY=layout.tabsY();
        addRenderableWidget(GuardUi.button("Items",left+8,tabY,tabWidth,20,()->{filtersOpen=false;rebuild();}));
        addRenderableWidget(GuardUi.button("Filters",left+12+tabWidth,tabY,tabWidth,20,()->{filtersOpen=true;rebuild();}));
        int bulkWidth=(panelWidth-26)/2;
        selectAllButton=addRenderableWidget(GuardUi.button("Select Results",left+8,layout.bulkY(),bulkWidth,20,()->selectAll(false)));
        clearResultsButton=addRenderableWidget(GuardUi.button("Clear Results",left+14+bulkWidth,layout.bulkY(),bulkWidth,20,this::clearResults));
        clearResultsButton.visible=!filtersOpen;clearResultsButton.active=!busy()&&!matches.isEmpty();
        clearResultsButton.setTooltip(GuardUi.tooltip("Remove both allowed and blocked assignments from every matching result.\nIncludes offscreen items; group selections receive exceptions."));
        selectAllButton.active=!busy()&&!matches.isEmpty();selectAllButton.visible=!filtersOpen;
        selectAllButton.setTooltip(GuardUi.tooltip(combined?"Select every matching item, including offscreen rows.\nLeft: allow; right: block.":"Assign every matching item, including offscreen rows."));
        if(filtersOpen)addFilters();
        else if(ruleMode&&(!countMode||combined)){
            int buttonWidth=Math.min(88,panelWidth/3);
            ruleInput=addRenderableWidget(GuardUi.editBox(font,left+10,layout.ruleY(),panelWidth-buttonWidth-30,"Assignment rule"));
            ruleInput.setMaxLength(256);ruleInput.setValue(ruleText);ruleInput.setHint(Component.literal("@mod, #tag, ID or !exception"));
            ruleInput.setResponder(text->{ruleText=text;updateRuleButton();});
            ruleInput.setTooltip(GuardUi.tooltip("Enter a group rule or ! exception.\nThis field assigns rules; Search only filters browsing."));
            ruleButton=addRenderableWidget(GuardUi.button(combined?"Assign Rule":ruleAction(),left+panelWidth-buttonWidth-14,layout.ruleY(),buttonWidth,20,()->{if(combined)assignRules(List.of(queryRule()),false,true);else toggleRule();}));
            updateRuleButton();
            ruleButton.setTooltip(GuardUi.tooltip(combined?"Assign the entered rule. Left: allow; right: block.\nEither click removes a rule already assigned.":"Assign or remove the entered rule."));
        }else if(showHandMode){
            addRenderableWidget(GuardUi.button("Hand: "+new String[]{"Held Items","Items + Empty Hands","Empty Hands Only"}[handMode],left+8,layout.ruleY(),panelWidth-22,20,()->{handMode=(handMode+1)%3;rebuild();})).setTooltip(GuardUi.tooltip("Held Items: eligible held items. Items + Empty Hands: either.\nEmpty Hands Only: no held items. Server eligibility still applies."));
        }
        if(showHandMode&&combined)addRenderableWidget(GuardUi.button("Hand Applicability",assignedLeft+8,layout.tabsY(),assignedWidth-22,20,()->{handMode=(handMode+1)%3;rebuild();})).setTooltip(GuardUi.tooltip("Currently: "+new String[]{"Held Items","Items + Empty Hands","Empty Hands Only"}[handMode]+".\nClick to change whether this preset also applies to empty hands."));
        if(countMode){
            countInput=addRenderableWidget(GuardUi.editBox(font,assignedLeft+assignedWidth-58,layout.tabsY(),40,"New item block limit"));
            countInput.setMaxLength(3);countInput.setValue(Integer.toString(assignmentCount));
            countInput.setFilter(GuardPoseItemScreen::validCountInput);
            countInput.setResponder(text->{try{assignmentCount=Integer.parseInt(text);}catch(NumberFormatException ignored){}});
            countInput.setTooltip(GuardUi.tooltip("New allowed items use this block limit.\n0 means unlimited; existing limits are editable below."));
        }
        addAssignedControls();
        int edge=assignedLeft+assignedWidth,actionWidth=layout.actionWidth();
        addRenderableWidget(GuardUi.button("Reset",left+8,layout.footerY(),actionWidth,20,()->GuardUi.confirm(this,"Reset assignments?",defaultAllowed==null?"Clear this screen's assignments and exceptions.\nApply saves the reset.":"Restore vanilla equipment with axes blocked.\nApply saves the reset.",this::resetAssignments)));
        addRenderableWidget(GuardUi.button("Apply",edge-2*actionWidth-12,layout.footerY(),actionWidth,20,()->apply(true)));
        addRenderableWidget(GuardUi.button("Close",edge-actionWidth-8,layout.footerY(),actionWidth,20,this::onClose));
    }
    private static boolean validCountInput(String text){return text.matches("[0-9]{0,3}")&&(text.isEmpty()||Integer.parseInt(text)<=dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT);}
    private void refreshEntries(){
        if(!combined){assignedEntries=List.copyOf(selected);return;}
        var entries=new ArrayList<String>();selected.forEach(value->entries.add("+ "+value));blocked.forEach(value->entries.add("- "+value));
        counts.keySet().stream().filter(id->!selected.contains(id)&&!blocked.contains(id)).forEach(id->entries.add("= "+id));
        assignedEntries=List.copyOf(entries);
    }
    private void addFilters(){
        int cell=(panelWidth-22)/2;
        for(int k=0;k<GuardItemSearch.CATEGORIES.length;k++){
            final int bit=1<<k;String category=GuardItemSearch.CATEGORIES[k];
            Button check=addRenderableWidget(GuardUi.button(font.plainSubstrByWidth(((categoryMask&bit)!=0?"[x] ":"[ ] ")+category,cell-8),left+8+(k%2)*(cell+4),gridTop()+(k/2)*22,cell,20,()->{categoryMask^=bit;filter();rebuild();}));
            check.setTooltip(GuardUi.tooltip(category+". Filter browsing only.\nChecked categories combine; no checks shows everything."));
        }
        addRenderableWidget(GuardUi.button("Clear",left+panelWidth-(panelWidth<190?72:80),layout.tabsY(),panelWidth<190?58:66,20,()->{categoryMask=0;filter="";filter();rebuild();}));
    }
    private String visibleEntry(int row) {
        int index=assignedScroll+row;
        return index<assignedEntries.size()?assignedEntries.get(index):null;
    }
    private void addAssignedControls(){
        rowRemovers.clear();rowCounts.clear();
        for(int r=0;r<assignedRows;r++){
            final int row=r;int y=assignedTop()+r*GuardUiLayout.ITEM_STEP;
            EditBox amount=null;
            if(countMode){
                amount=addRenderableWidget(GuardUi.editBox(font,assignedLeft+assignedWidth-78,y,32,"Item block limit"));
                amount.setMaxLength(3);amount.setFilter(GuardPoseItemScreen::validCountInput);amount.setHint(Component.literal("–"));
                amount.setResponder(text->{
                    if(bindingRows)return;
                    String entry=visibleEntry(row);if(entry==null)return;String id=entryRule(entry);
                    if(text.isEmpty()){counts.remove(id);return;}
                    try{setCount(id,Integer.parseInt(text));}catch(NumberFormatException ignored){}
                });
                amount.setTooltip(GuardUi.tooltip("Block limit: 0–100; 0 is unlimited."));
            }
            rowCounts.add(amount);
            rowRemovers.add(addRenderableWidget(GuardUi.button("x",assignedLeft+assignedWidth-38,y,20,20,()->{
                String entry=visibleEntry(row);if(entry!=null)removeAssignment(entry);
            })));
        }
        bindAssignedRows();
    }
    private void bindAssignedRows(){
        bindingRows=true;
        try{
            for(int r=0;r<rowRemovers.size();r++){
                String entry=visibleEntry(r);Button remove=rowRemovers.get(r);remove.visible=entry!=null;
                if(entry!=null)remove.setTooltip(GuardUi.tooltip("Remove "+entry));
                EditBox amount=rowCounts.get(r);
                if(amount!=null){
                    String id=entry==null?null:entryRule(entry);amount.visible=id!=null&&itemById.containsKey(id);
                    amount.setValue(amount.visible&&counts.containsKey(id)?Integer.toString(counts.get(id)):"");
                }
            }
        }finally{bindingRows=false;}
    }
    private boolean over(Button button,double x,double y){return button!=null&&button.visible&&button.active&&x>=button.getX()&&x<button.getX()+button.getWidth()&&y>=button.getY()&&y<button.getY()+button.getHeight();}
    @Override public boolean mouseClicked(double x,double y,int b){
        if(b==0&&beginScrollDrag(x,y))return true;
        if(bulkEdit!=null)return true;
        if(b==1&&combined){if(over(selectAllButton,x,y)){selectAll(true);return true;}if(over(ruleButton,x,y)){if(validQuery())assignRules(List.of(queryRule()),true,true);return true;}}
        if(!filtersOpen&&(b==0||b==1)&&y>=gridTop()&&y<gridTop()+rows*GuardUiLayout.ITEM_STEP&&x>=left+8&&x<left+8+columns*GuardUiLayout.ITEM_STEP){
            int c=(int)(x-left-8)/GuardUiLayout.ITEM_STEP,r=(int)(y-gridTop())/GuardUiLayout.ITEM_STEP,i=(scroll+r)*columns+c;
            if(i<matches.size()){
                String id=BuiltInRegistries.ITEM.getKey(matches.get(i)).toString();
                if(clearItemAssignment(id,true))return true;
                if(combined){assignRules(List.of(id),b==1);return true;}
                if(countMode){try{int count=Integer.parseInt(countInput.getValue());if(count<0||count>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_COUNT)throw new NumberFormatException();setCount(id,count);}catch(NumberFormatException invalid){notice="Enter a count from 0 to 100.";}return true;}
                if(!selected.remove(id)){if(ruleMode&&selected(matches.get(i))){notice="Selected by a group rule. Remove that rule to exclude it.";return true;}boolean exception=ruleMode&&selected.remove("!"+id);selected.add(id);if(ruleMode&&rules().length()>GuardItemRules.MAX_LENGTH){selected.remove(id);if(exception)selected.add("!"+id);notice="Assignment list is full. Remove unused rules.";}}
                rebuild();return true;
            }
        }
        return super.mouseClicked(x,y,b);
    }
    private boolean clearItemAssignment(String id,boolean inherited){
        Item item=itemById.get(id);
        boolean groupRules=ruleMode&&(!countMode||combined)&&item!=null;
        boolean assigned=selected.contains(id)||blocked.contains(id)||counts.containsKey(id)
            ||inherited&&groupRules&&(selected.compiled().matches(item.getDefaultInstance())
                ||blocked.compiled().matches(item.getDefaultInstance()));
        if(!assigned)return false;
        var oldAllowed=new LinkedHashSet<>(selected);var oldBlocked=new LinkedHashSet<>(blocked);
        var oldCounts=new LinkedHashMap<>(counts);
        selected.remove(id);blocked.remove(id);counts.remove(id);
        selected.remove("!"+id);blocked.remove("!"+id);
        // Removing an exact rule must also remove selection inherited from broader rules.
        if(groupRules){
            if(selected.compiled().matches(item.getDefaultInstance()))selected.add("!"+id);
            if(blocked.compiled().matches(item.getDefaultInstance()))blocked.add("!"+id);
        }
        if(ruleMode&&(!countMode||combined)&&(!GuardItemRules.valid(rules())||!GuardItemRules.valid(blocked.text()))){
            selected.clear();selected.addAll(oldAllowed);blocked.clear();blocked.addAll(oldBlocked);
            counts.clear();counts.putAll(oldCounts);notice="No room for the item exception. Remove an unused rule first.";
            return true;
        }
        rebuild();return true;
    }
    private void selectAll(boolean blacklist){
        if(busy())return;
        List<String> ids=matches.stream().map(item->BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        if(!countMode)ids=compactResults(ids);
        if(combined){assignRules(ids,blacklist);return;}
        int count=assignmentCount;
        if(countMode){try{count=Integer.parseInt(countInput.getValue());if(count<0||count>100)throw new NumberFormatException();}catch(NumberFormatException bad){notice="Enter a count from 0 to 100.";return;}}
        final int value=count;final var entries=ids;
        var allowed=new dev.zeli.mallardguard.GuardItemAssignments.Draft(selected);var denied=new dev.zeli.mallardguard.GuardItemAssignments.Draft(blocked);var limits=new LinkedHashMap<>(counts);
        startBulk(new BulkEdit(){ {total=entries.size();}
            boolean step(){if(cursor>=total)return true;String id=entries.get(cursor++);allowed.add(id);if(countMode)limits.put(id,value);return cursor==total;}
            void finish(){replaceAssignments(allowed,denied,limits);}
        });
    }
    private void clearResults(){
        if(busy())return;
        var allowed=new dev.zeli.mallardguard.GuardItemAssignments.Draft(selected);var denied=new dev.zeli.mallardguard.GuardItemAssignments.Draft(blocked);var limits=new LinkedHashMap<>(counts);
        // Only the unrestricted, unfiltered registry browser means "clear everything".
        if(matches.size()==itemById.size()){allowed.clear();denied.clear();limits.clear();replaceAssignments(allowed,denied,limits);return;}
        var results=List.copyOf(matches);
        var wholeMods=new HashSet<String>();
        for(String rule:compactResults(results.stream().map(item->metadata.get(item).id).toList()))if(rule.startsWith("@"))wholeMods.add(rule.substring(1));
        var allowedGroups=GuardItemRules.compile(allowed.stream().filter(q->!q.contains(":" )||q.startsWith("#")||q.startsWith("!#")).collect(java.util.stream.Collectors.joining(",")));
        var deniedGroups=GuardItemRules.compile(denied.stream().filter(q->!q.contains(":" )||q.startsWith("#")||q.startsWith("!#")).collect(java.util.stream.Collectors.joining(",")));
        startBulk(new BulkEdit(){ {total=results.size();}
            boolean step(){if(cursor>=total)return true;Item item=results.get(cursor++);String id=metadata.get(item).id;allowed.remove(id);denied.remove(id);limits.remove(id);allowed.remove("!"+id);denied.remove("!"+id);
                if(ruleMode&&(!countMode||combined)){String exception=wholeMods.contains(metadata.get(item).namespace)?"!@"+metadata.get(item).namespace:"!"+id;if(allowedGroups.matches(item.getDefaultInstance()))allowed.add(exception);if(deniedGroups.matches(item.getDefaultInstance()))denied.add(exception);}return cursor==total;}
            void finish(){replaceAssignments(allowed,denied,limits);}
        });
    }
    private void replaceAssignments(Collection<String> allowed,Collection<String> denied,Map<String,Integer> limits){
        if(!GuardItemRules.valid(String.join(",",allowed))||!GuardItemRules.valid(String.join(",",denied))||dev.zeli.mallardguard.GuardItemBlockCounts.encode(limits).length()>dev.zeli.mallardguard.GuardItemBlockCounts.MAX_LENGTH){notice="The edit exceeds the saved-file size limit. Current assignments were kept.";return;}
        selected.clear();selected.addAll(allowed);blocked.clear();blocked.addAll(denied);counts.clear();counts.putAll(limits);rebuild();
    }
    private List<String> compactResults(List<String> ids){
        if(!ruleMode)return ids;var wanted=new HashSet<>(ids);var namespaces=new LinkedHashMap<String,List<String>>();
        namespaces.putAll(namespaceItems);
        var compact=new ArrayList<String>();for(var entry:namespaces.entrySet()){if(wanted.containsAll(entry.getValue())&&entry.getValue().size()>1){compact.add("@"+entry.getKey());wanted.removeAll(entry.getValue());}}
        for(String id:ids)if(wanted.contains(id))compact.add(id);return compact;
    }
    private void assignRules(List<String> entries,boolean blacklist){assignRules(entries,blacklist,false);}
    private void assignRules(List<String> entries,boolean blacklist,boolean toggle){
        if(entries.isEmpty()||entries.stream().anyMatch(q->!GuardItemRules.valid(q)||q.isBlank()||q.contains(",")))return;
        var opposite=blacklist?selected:blocked;
        if(countMode&&!blacklist&&countInput!=null&&countInput.getValue().isEmpty()){notice="Enter a count from 0 to 100.";return;}
        if(toggle&&entries.size()==1&&clearItemAssignment(entries.get(0),false))return;
        String oppositeRules=opposite.text();
        GuardItemRules.Compiled oppositeCompiled=opposite.compiled();
        // Exact IDs use the existing registry index; no opposite rules means no overlap scan.
        var candidate=GuardItemRules.compile(String.join(",",entries));var results=List.copyOf(all);
        startBulk(new BulkEdit(){int conflicts;String example=""; {total=opposite.isEmpty()?0:results.size();}
            boolean step(){if(cursor>=total)return true;Item item=results.get(cursor++);var stack=item.getDefaultInstance();if(candidate.matches(stack)&&oppositeCompiled.matches(stack)){conflicts++;if(example.isEmpty())example=metadata.get(item).id;}return cursor==total;}
            void finish(){if(conflicts==0){commitRules(entries,blacklist,false);return;}String label=blacklist?"Block":"Allow";
                GuardUi.choices(GuardPoseItemScreen.this,"Conflicting item rules",conflicts+" item(s) overlap the "+(blacklist?"whitelist":"blacklist")+".\n\nExample: "+example+"\nMatching rules: "+font.plainSubstrByWidth(oppositeRules,250)+"\n\n"+label+" adds ! exceptions to the opposite list.\nBroader mod, tag and keyword rules stay in place.",new GuardUi.Choice(label+" Items",()->commitRules(entries,blacklist,true)),new GuardUi.Choice("Keep Existing",()->Minecraft.getInstance().setScreen(GuardPoseItemScreen.this)),new GuardUi.Choice("Cancel",()->Minecraft.getInstance().setScreen(GuardPoseItemScreen.this)));}
        });
    }
    private void commitRules(List<String> entries,boolean blacklist,boolean exceptions){
        var allowed=new dev.zeli.mallardguard.GuardItemAssignments.Draft(selected);var denied=new dev.zeli.mallardguard.GuardItemAssignments.Draft(blocked);var limits=new LinkedHashMap<>(counts);
        var target=blacklist?denied:allowed;var opposite=blacklist?allowed:denied;
        startBulk(new BulkEdit(){ {total=entries.size();}
            boolean step(){if(cursor>=total)return true;String q=entries.get(cursor++);if(countMode&&!blacklist&&itemById.containsKey(q))limits.put(q,assignmentCount);target.remove(q.startsWith("!")?q.substring(1):"!"+q);target.add(q);if(!q.startsWith("!")){opposite.remove(q);if(exceptions)opposite.add("!"+q);}return cursor==total;}
            void finish(){replaceAssignments(allowed,denied,limits);Minecraft.getInstance().setScreen(GuardPoseItemScreen.this);}
        });Minecraft.getInstance().setScreen(this);
    }
    private void help(){
        String text=countMode&&!combined?"Set a count from 0–100, then click an unassigned item. 0 means unlimited. Click an assigned item to remove its override, or edit its count in Assigned. Select Results uses that count for every match. Item overrides take priority over compatibility and default limits.":combined?"Left-click to allow an item; right-click to block it. Click an assigned item to remove it; group rules get an !item_id exception. The blacklist takes priority. Select Results includes offscreen matches; Clear Results removes their assignments.\n\nSearch filters the browser. Enter rules separately: @minecraft for a mod, #minecraft:swords for a tag, minecraft:stone for an item, or tacz for text in IDs and tag IDs. Prefix a rule with ! to exclude matches from its own list.":"Click an item to assign or remove it. Group rules get an !item_id exception. Select Results includes every match; Clear Results removes their assignments.\n\nSearch names, IDs, tooltip text, #tags or @mods. Filters change only the browser; Assigned shows all entries.";
        if(defaultAllowed!=null)text="These assignments control generated mob equipment. Vanilla equipment is allowed by default, except axes.\n\n"+text;
        if(countMode&&combined)text+="\n\nNew allowed items use the limit beside Assigned. Edit individual limits below; blank uses the default and 0 means unlimited. Group rules do not assign limits.";
        GuardUi.choices(this,"Assignment Help",text,new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)));
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(bulkEdit!=null){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean charTyped(char c,int modifiers){return bulkEdit!=null||super.charTyped(c,modifiers);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(x>=assignedLeft&&x<assignedLeft+assignedWidth&&y>=assignedTop()&&y<assignedTop()+assignedRows*GuardUiLayout.ITEM_STEP){
            int next=Math.max(0,Math.min(assignedScroll-(int)Math.signum(sy),maxAssignedScroll()));
            if(next!=assignedScroll){assignedScroll=next;bindAssignedRows();}return true;
        }
        if(!filtersOpen&&x>=left&&x<left+panelWidth&&y>=gridTop()&&y<gridTop()+rows*GuardUiLayout.ITEM_STEP){scroll=Math.max(0,Math.min(scroll-(int)Math.signum(sy),maxScroll()));return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override public void render(GuiGraphics g,int x,int y,float d){
        g.flush();renderBackground(g,x,y,d);g.flush();
        int available=assignedLeft+assignedWidth-left;
        GuardUi.panel(g,left-4,top-5,available+8,bottom-top+5,false);
        GuardUi.panel(g,assignedLeft,top+32,assignedWidth,layout.contentBottom()-top-32,true);
        g.drawString(font,font.plainSubstrByWidth(ruleMode?ruleTitle:"Assign Items",available-50),left+8,top+7,GuardUi.TEXT);
        g.fill(left+4,top+29,left+available-4,top+30,GuardUi.BORDER);
        g.fill(assignedLeft-6,top+32,assignedLeft-5,bottom-56,GuardUi.BORDER);
        g.drawString(font,"Assigned ("+assignedEntries.size()+")",assignedLeft+8,top+40,GuardUi.TEXT);
        Item hovered=null,assignedHovered=null;String hoveredRule=null;
        GuardItemRules.Compiled allowedRules=selected.compiled(),blockedRules=blocked.compiled();
        if(!filtersOpen)for(int r=0;r<rows;r++)for(int c=0;c<columns;c++){
            int i=(scroll+r)*columns+c;if(i>=matches.size())continue;
            Item item=matches.get(i);int px=left+8+c*GuardUiLayout.ITEM_STEP,py=gridTop()+r*GuardUiLayout.ITEM_STEP;
            boolean over=x>=px&&x<px+21&&y>=py&&y<py+21;
            boolean blockedMatch=combined&&blockedRules.matches(item.getDefaultInstance()),allowedMatch=ruleMode&&(!countMode||combined)?allowedRules.matches(item.getDefaultInstance()):selected.contains(BuiltInRegistries.ITEM.getKey(item).toString());
            g.fill(px,py,px+21,py+21,blockedMatch?0xAA823B48:allowedMatch?0xAA55735C:0x77211B2A);
            g.renderOutline(px,py,21,21,over?GuardUi.HIGHLIGHT:GuardUi.BORDER);
            g.renderItem(item.getDefaultInstance(),px+2,py+2);if(over)hovered=item;
        }
        if(assignedEntries.isEmpty())g.drawString(font,"No assignments",assignedLeft+8,assignedTop()+6,GuardUi.MUTED);
        for(int r=0;r<assignedRows&&assignedScroll+r<assignedEntries.size();r++){
            String stored=assignedEntries.get(assignedScroll+r),entry=entryRule(stored);int py=assignedTop()+r*GuardUiLayout.ITEM_STEP;
            Item item=itemById.get(entry);int labelEnd=assignedLeft+assignedWidth-(countMode?84:44);
            g.fill(assignedLeft+6,py,labelEnd,py+21,0x77211B2A);
            if(combined)g.fill(assignedLeft+6,py,assignedLeft+8,py+21,blockedEntry(stored)?0xFFE79999:0xFFADD4AE);
            int tx=assignedLeft+11,limit=Math.max(0,labelEnd-tx-3);
            if(item!=null){g.renderItem(item.getDefaultInstance(),tx,py+2);tx+=20;limit=Math.max(0,limit-20);g.drawString(font,font.plainSubstrByWidth(item.getDescription().getString(),limit),tx,py+2,GuardUi.TEXT);g.drawString(font,font.plainSubstrByWidth(combined?(stored.startsWith("= ")?"Limit override":blockedEntry(stored)?"Blocked":"Allowed"):entry,limit),tx,py+12,GuardUi.MUTED);}
            else g.drawString(font,font.plainSubstrByWidth(ruleLabel(entry),limit),tx,py+6,GuardUi.TEXT);
            if(x>=assignedLeft+6&&x<labelEnd&&y>=py&&y<py+21){hoveredRule=assignmentLabel(stored);assignedHovered=item;}
        }
        if(!filtersOpen)renderScrollbar(g,1,x,y);
        renderScrollbar(g,2,x,y);
        if(countMode)g.drawString(font,font.plainSubstrByWidth("New item limit",assignedWidth-76),assignedLeft+8,layout.tabsY()+6,GuardUi.MUTED);
        String hint=bulkEdit!=null?"Checking assignments: "+bulkEdit.cursor+" / "+bulkEdit.total:indexing?(tooltipQuery()?"Indexing tooltips: ":"Searching items: ")+tooltipCursor+" / "+all.size():searchDeadline!=0?"Updating search…":notice.isEmpty()?(filtersOpen?"Filters affect browsing only.":combined?"Left: allow | Right: block | Click again: remove":countMode?"Click again to remove. Edit saved limits in Assigned.":"Either click removes an assigned item."):notice;
        g.drawString(font,font.plainSubstrByWidth(hint,available-16),left+8,bottom-46,notice.isEmpty()?GuardUi.MUTED:0xFFE79999);
        g.flush();for(var widget:renderables)widget.render(g,x,y,d);
        int tabWidth=layout.tabWidth(),tabX=left+8+(filtersOpen?tabWidth+4:0);
        g.fill(tabX+2,top+82,tabX+tabWidth-2,top+84,GuardUi.HIGHLIGHT);
        if(hovered!=null){var lines=new ArrayList<>(tooltip(hovered));String id=BuiltInRegistries.ITEM.getKey(hovered).toString();boolean denied=combined&&blockedRules.matches(hovered.getDefaultInstance());boolean included=selected(hovered);lines.add(Component.literal(denied?"Blacklisted":included?"Whitelisted":"Unassigned").withStyle(denied?net.minecraft.ChatFormatting.RED:included?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.GRAY));g.renderTooltip(font,lines.stream().map(Component::getVisualOrderText).toList(),x,y);}
        else if(hoveredRule!=null){var lines=assignedHovered==null?new ArrayList<Component>():new ArrayList<>(tooltip(assignedHovered));lines.add(Component.literal(hoveredRule));g.renderTooltip(font,lines.stream().map(Component::getVisualOrderText).toList(),x,y);}
    }
    private int barX(int bar){return (bar==1?left+panelWidth:assignedLeft+assignedWidth)-12;}
    private int barTop(int bar){return bar==1?gridTop():assignedTop();}
    private int barRows(int bar){return bar==1?rows:assignedRows;}
    private int barMax(int bar){return bar==1?maxScroll():maxAssignedScroll();}
    private int barOffset(int bar){return bar==1?scroll:assignedScroll;}
    private int trackHeight(int bar){return barRows(bar)*GuardUiLayout.ITEM_STEP-2;}
    private GuardUiLayout.Scrollbar scrollbar(int bar){return new GuardUiLayout.Scrollbar(barX(bar),barTop(bar),trackHeight(bar),barRows(bar),barRows(bar)+barMax(bar),barOffset(bar));}
    private boolean beginScrollDrag(double x,double y){
        for(int bar=1;bar<=2;bar++){
            if(bar==1&&filtersOpen||barMax(bar)==0)continue;
            if(!scrollbar(bar).contains(x,y))continue;
            dragBar=bar;dragGrab=scrollbar(bar).grab(y);
            moveScrollDrag(y);return true;
        }
        return false;
    }
    private void moveScrollDrag(double y){
        int next=scrollbar(dragBar).scrollAt(y,dragGrab);
        if(dragBar==1)scroll=next;
        else if(assignedScroll!=next){assignedScroll=next;bindAssignedRows();}
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(button==0&&dragBar!=0){moveScrollDrag(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&dragBar!=0){dragBar=0;return true;}
        return super.mouseReleased(x,y,button);
    }
    private void renderScrollbar(GuiGraphics g,int bar,int mouseX,int mouseY){GuardUi.scrollbar(g,scrollbar(bar),mouseX,mouseY,dragBar==bar);}
    @Override public void onClose(){if(bulkEdit!=null){bulkEdit=null;notice="Pending edit cancelled. Current assignments were kept.";updateBulkButtons();return;}if(handMode!=saved.handMode()||!selected.equals(new LinkedHashSet<>(saved.items()))||countMode&&!countRules().equals(savedCounts)||combined&&!blocked.equals(savedBlocked))GuardUi.choices(this,"Unsaved assignments","Apply assignments or discard these edits.",new GuardUi.Choice("Apply & Close",()->apply(true)),new GuardUi.Choice("Discard",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));else Minecraft.getInstance().setScreen(parent);}
    @Override public void removed(){dragBar=0;super.removed();}
    @Override public boolean isPauseScreen(){return parent.isPauseScreen();}
}
