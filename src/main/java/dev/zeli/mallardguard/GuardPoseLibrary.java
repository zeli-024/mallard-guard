package dev.zeli.mallardguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.neoforged.fml.loading.FMLPaths;

/** Named client poses with atomic writes for changed files. */
public final class GuardPoseLibrary {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson WIRE_JSON = new Gson();
    private static List<Pose> saved = List.of();
    private static long generation;
    public static final class Pose {
        public String id,name;
        public boolean enabled,preset,deleted;
        public int[] values,parriedValues;
        public int parriedHoldMs=100;
        public List<String> items=new ArrayList<>();
        public List<String> excludedItems=new ArrayList<>(),otherHandExcluded=new ArrayList<>(),weaponExcluded=new ArrayList<>(),exclusiveItems=new ArrayList<>();
        private transient GuardItemAssignments.Rules selectionRules,supportRules,weaponRules;
        private transient GuardItemRules.Compiled exclusiveRules;
        public int handMode;
        public boolean otherHandHeldOnly;
        public boolean maintainHeld=true;
        public List<String> otherHandItems=new ArrayList<>(),weaponItems=new ArrayList<>();
        public void prepareRules(){selectionRules=new GuardItemAssignments.Rules(items,excludedItems);supportRules=new GuardItemAssignments.Rules(otherHandItems,otherHandExcluded);weaponRules=new GuardItemAssignments.Rules(weaponItems,weaponExcluded);exclusiveRules=GuardItemRules.compile(String.join(",",exclusiveItems));}
        public boolean matches(net.minecraft.world.item.ItemStack item,boolean eligible){
            if(selectionRules==null)prepareRules();
            return item.isEmpty()?handMode==1||handMode==2:handMode!=2&&selectionRules.allows(item,eligible);
        }
        public boolean supporting(net.minecraft.world.item.ItemStack item,boolean fallback){if(supportRules==null)prepareRules();return supportRules.allows(item,fallback);}
        public boolean weapon(net.minecraft.world.item.ItemStack item){if(weaponRules==null)prepareRules();return weaponRules.allows(item,true);}
        public boolean exclusive(net.minecraft.world.item.ItemStack item){if(exclusiveRules==null)prepareRules();return !item.isEmpty()&&exclusiveRules.matches(item)&&selectionRules.allows(item,false);}
        public Pose copy(){Pose p=new Pose();p.id=id;p.name=name;p.enabled=enabled;p.preset=preset;p.deleted=deleted;p.values=values.clone();p.parriedValues=parriedValues.clone();p.parriedHoldMs=parriedHoldMs;p.items=new ArrayList<>(items);p.handMode=handMode;p.otherHandHeldOnly=otherHandHeldOnly;p.maintainHeld=maintainHeld;p.otherHandItems=new ArrayList<>(otherHandItems);p.weaponItems=new ArrayList<>(weaponItems);p.excludedItems=new ArrayList<>(excludedItems);p.otherHandExcluded=new ArrayList<>(otherHandExcluded);p.weaponExcluded=new ArrayList<>(weaponExcluded);p.exclusiveItems=new ArrayList<>(exclusiveItems);return p;}
    }
    public static boolean samePose(Pose a,Pose b){
        return a==b || a!=null && b!=null && Objects.equals(a.id,b.id) && Objects.equals(a.name,b.name)
            && a.enabled==b.enabled && a.preset==b.preset && a.deleted==b.deleted && a.handMode==b.handMode
            && a.otherHandHeldOnly==b.otherHandHeldOnly && a.maintainHeld==b.maintainHeld && a.parriedHoldMs==b.parriedHoldMs
            && Arrays.equals(a.values,b.values) && Arrays.equals(a.parriedValues,b.parriedValues) && Objects.equals(a.items,b.items)
            && Objects.equals(a.excludedItems,b.excludedItems)&&Objects.equals(a.otherHandExcluded,b.otherHandExcluded)&&Objects.equals(a.weaponExcluded,b.weaponExcluded)&&Objects.equals(a.exclusiveItems,b.exclusiveItems)
            && Objects.equals(a.otherHandItems,b.otherHandItems) && Objects.equals(a.weaponItems,b.weaponItems);
    }
    public static boolean samePoses(List<Pose> a,List<Pose> b){
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++)if(!samePose(a.get(i),b.get(i)))return false;
        return true;
    }
    public static final int MAX_POSES=128;
    private static final int MAX_POSE_BYTES=4_194_304;
    public static final int MAX_ANIMATIONS_BYTES=8_388_608;
    public static Pose create(){Pose p=preset(0);p.id="pose_"+UUID.randomUUID().toString().replace("-","");p.name="";p.preset=false;p.values=new int[23];p.values[0]=1;p.values[19]=150;p.values[20]=360;p.values[21]=3;p.values[22]=100;p.parriedValues=GuardPoseSettings.defaultParriedPose(p.values);return p;}
    private GuardPoseLibrary() {}
    public static Path folder() { return FMLPaths.CONFIGDIR.get().resolve("mallard_guard/mg_punchy"); }
    public static synchronized long generation() { return generation; }
    public static synchronized List<Pose> snapshot() { return saved.stream().map(Pose::copy).toList(); }
    public static Pose preset(int i) {
        Pose p = new Pose(); p.id = "preset" + (i + 1); p.name = "Preset " + (i + 1);
        p.preset = true; p.values = GuardPoseSettings.defaultPose(i); p.enabled = p.values[0] != 0; p.handMode = i == 4 ? 2 : 0; p.parriedValues=GuardPoseSettings.defaultParriedPose(p.values); return p;
    }
    public static synchronized void prepare() {
        try {
            Files.createDirectories(folder().resolve("poses"));
            Path destination = folder().resolve("config.toml"), legacy = FMLPaths.CONFIGDIR.get().resolve("mallard_guard/client.toml");
            if (!Files.exists(destination) && Files.isRegularFile(legacy)) {
                try (CommentedFileConfig from = CommentedFileConfig.builder(legacy).sync().build();
                     CommentedFileConfig to = CommentedFileConfig.builder(destination).sync().build()) {
                    from.load();
                    Object enabled = from.get("punchy.enabled"); if (enabled instanceof Boolean) to.set("enabled", enabled);
                    for (int i = 0; i < GuardPoseSettings.COUNT; i++) {
                        Object raw = from.get("punchy.pose" + (i + 1));
                        if (raw instanceof String text && GuardPoseSettings.parsePose(text) != null) to.set("pose" + (i + 1), text);
                    }
                    to.save();
                }
            }
        } catch (IOException | RuntimeException error) { org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Could not prepare Punchy pose storage", error); }
    }
    public static synchronized void loaded() {
        List<Pose> found = new ArrayList<>();
        for (int i=0;i<GuardPoseSettings.COUNT;i++) {
            Pose p=preset(i);int[] old=GuardPoseSettings.parsePose(GuardConfig.PUNCHY_POSES.get(i).get());
            if(old!=null){p.values=old;p.enabled=old[0]!=0;p.parriedValues=GuardPoseSettings.defaultParriedPose(old);}found.add(p);
        }
        Set<String> loadedIds=new HashSet<>();
        try(var files=Files.list(folder().resolve("poses"))){
            // Named files take precedence over legacy files with an internal ID as their name.
            var ordered=files.filter(f->Files.isRegularFile(f)&&f.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing((Path f)->f.getFileName().toString().matches("(preset[1-5]|pose_[a-f0-9]{32})\\.json")).thenComparing(f->f.getFileName().toString())).toList();
            for(Path file:ordered){
                try{
                    if(Files.size(file)>MAX_POSE_BYTES)continue;Pose p=readPose(Files.readString(file));
                    if(!valid(p)||!loadedIds.add(p.id))continue;
                    if(p.preset)found.set(Integer.parseInt(p.id.substring(6))-1,p);
                    else if(found.size()<MAX_POSES)found.add(p);
                }catch(IOException|RuntimeException error){org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Could not read pose {}",file.getFileName(),error);}
            }
        }catch(IOException error){org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Could not read poses",error);}
        for (int i=0;i < GuardPoseSettings.COUNT;i++) { Pose p=found.get(i);p.values[0]=p.enabled?1:0;GuardConfig.PUNCHY_POSES.get(i).set(GuardPoseSettings.encodePose(p.values)); }
        if(!save(found)){saved=found.stream().map(Pose::copy).toList();generation++;}
        GuardConfig.PUNCHY_SPEC.save();

    }
    private static Pose readPose(String text) {
        var tree=com.google.gson.JsonParser.parseString(text).getAsJsonObject();
        Pose pose=JSON.fromJson(tree,Pose.class);
        if(tree.has("variants")){
            var variants=tree.getAsJsonObject("variants");
            if(variants.has("parrying")){var parry=variants.getAsJsonObject("parrying");
                if(parry.has("values"))pose.values=JSON.fromJson(parry.get("values"),int[].class);
                if(parry.has("enabled"))pose.enabled=parry.get("enabled").getAsBoolean();
                if(parry.has("items"))pose.items=JSON.fromJson(parry.get("items"),new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                if(parry.has("handMode"))pose.handMode=parry.get("handMode").getAsInt();
            }
        }
        if(!tree.has("maintainHeld"))pose.maintainHeld=true;
        if(pose.excludedItems==null)pose.excludedItems=new ArrayList<>();if(pose.otherHandExcluded==null)pose.otherHandExcluded=new ArrayList<>();if(pose.weaponExcluded==null)pose.weaponExcluded=new ArrayList<>();if(pose.exclusiveItems==null)pose.exclusiveItems=new ArrayList<>();
        if(pose.items==null)pose.items=new ArrayList<>();
        if(pose.otherHandItems==null)pose.otherHandItems=new ArrayList<>();
        if(pose.weaponItems==null)pose.weaponItems=new ArrayList<>();
        if(!tree.has("handMode")&&tree.has("hand")&&tree.get("hand").getAsBoolean())pose.handMode=pose.items.isEmpty()?2:1;
        int[] origin=tree.has("origin")?JSON.fromJson(tree.get("origin"),int[].class):null;
        if(pose.values==null && origin!=null && origin.length==23 && tree.has("adjustments")) {
            pose.values=origin.clone();var offsets=tree.getAsJsonArray("adjustments");
            if(offsets.size()!=18)return null;
            for(int i=1;i<=18;i++){double n=offsets.get(i-1).getAsDouble();if(!Double.isFinite(n)||n < -((i-1)%6<3?20:180)||n > ((i-1)%6<3?20:180))return null;int scale=(i-1)%6<3?10:1;pose.values[i]=Math.clamp(origin[i]+(int)Math.round(n*scale),GuardPoseSettings.min(i),GuardPoseSettings.max(i));}
        }
        if(tree.has("motion")&&pose.values!=null){int[] motion=JSON.fromJson(tree.get("motion"),int[].class);if(motion.length!=4)return null;System.arraycopy(motion,0,pose.values,19,4);}
        if(pose.values!=null){int[] normalized=GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(pose.values));if(normalized==null)return null;pose.values=normalized;}
        if(pose.values!=null && pose.parriedValues==null && !tree.has("parriedValues"))
            pose.parriedValues=GuardPoseSettings.defaultParriedPose(pose.values);
        if(pose.parriedValues!=null){
            int[] normalized=GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(pose.parriedValues));
            if(normalized==null)return null;
            pose.parriedValues=normalized;
        }
        if(!tree.has("parriedHoldMs"))pose.parriedHoldMs=100;
        return pose;
    }
    private static String writePose(Pose p) {
        var tree=JSON.toJsonTree(p).getAsJsonObject();
        return JSON.toJson(tree);
    }
    private static boolean validItems(List<String> items){return items!=null&&items.stream().allMatch(java.util.Objects::nonNull)&&GuardItemRules.valid(String.join(",",items));}
    private static boolean valid(Pose p){return p!=null&&p.id!=null&&(p.preset?p.id.matches("preset[1-5]"):p.id.matches("pose_[a-f0-9]{32}"))&&(!p.deleted||!p.enabled&&p.exclusiveItems.isEmpty())&&p.name!=null&&!p.name.isBlank()&&p.name.length()<=64&&p.handMode>=0&&p.handMode<=2&&p.parriedHoldMs>=0&&p.parriedHoldMs<=500&&validItems(p.items)&&validItems(p.excludedItems)&&validItems(p.otherHandExcluded)&&validItems(p.weaponExcluded)&&validItems(p.exclusiveItems)&&validItems(p.otherHandItems)&&validItems(p.weaponItems)&&p.values!=null&&GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(p.values))!=null&&p.parriedValues!=null&&GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(p.parriedValues))!=null;}
    public static synchronized boolean save(List<Pose> poses) {
        if(poses.size()<GuardPoseSettings.COUNT||poses.size()>MAX_POSES||poses.stream().anyMatch(p->!valid(p)))return false;
        for(Pose p:poses){p.values=GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(p.values));p.parriedValues=GuardPoseSettings.parsePose(GuardPoseSettings.encodePose(p.parriedValues));}
        Set<String> unique=new HashSet<>();for(Pose p:poses)if(!unique.add(p.id))return false;
        for(int i=1;i<=GuardPoseSettings.COUNT;i++)if(!unique.contains("preset"+i))return false;
        // Check every serialized pose before writing any file. The reader uses the same limit.
        Map<String,String> serialized=new HashMap<>();
        for(Pose p:poses){String text=writePose(p);if(text.getBytes(StandardCharsets.UTF_8).length>MAX_POSE_BYTES)return false;serialized.put(p.id,text);}
        try {
            Path directory = folder().resolve("poses"); Files.createDirectories(directory);
            Set<String> ids = new HashSet<>(), names=new HashSet<>();
            Set<Path> written=new HashSet<>();
            for (Pose p : poses) {
                if (!ids.add(p.id)) return false;
                String name=p.name.replaceAll("[\\\\/:*?\"<>|]","_").strip();if(name.isBlank())name=p.id;
                String fileName=name+".json";int duplicate=0;while(!names.add(fileName.toLowerCase(Locale.ROOT)))fileName=name+" - "+p.id+(duplicate++==0?"":" - "+duplicate)+".json";
                Path target=directory.resolve(fileName),temporary=directory.resolve(p.id+".tmp");written.add(target);
                String contents = serialized.get(p.id);
                if (Files.isRegularFile(target) && Files.size(target) <= MAX_POSE_BYTES
                    && contents.equals(Files.readString(target, StandardCharsets.UTF_8))) continue;
                GuardConfig.preserveClientFile(target);
                Files.writeString(temporary, contents, StandardCharsets.UTF_8);
                try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            }
            // Remove old names only after every new pose has been written successfully.
            Set<String> managed=new HashSet<>(ids);for(Pose old:saved)managed.add(old.id);
            try(var files=Files.list(directory)){
                for(Path file:files.filter(f->Files.isRegularFile(f)&&f.getFileName().toString().endsWith(".json")&&!written.contains(f)).toList()){
                    try{if(Files.size(file)<=MAX_POSE_BYTES){Pose old=readPose(Files.readString(file));if(valid(old)&&managed.contains(old.id)){GuardConfig.preserveClientFile(file);Files.delete(file);}}}catch(RuntimeException ignored){}
                }
            }
            saved = poses.stream().map(Pose::copy).toList(); generation++; return true;
        } catch (IOException error) { org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Could not save Punchy poses", error); return false; }
    }
    public static String encodeAnimations(List<Pose> poses){return WIRE_JSON.toJson(poses);}
    public static List<Pose> parseAnimations(String text){
        if(text==null||text.length()>MAX_ANIMATIONS_BYTES||text.getBytes(StandardCharsets.UTF_8).length>MAX_ANIMATIONS_BYTES)return null;
        try{
            var array=com.google.gson.JsonParser.parseString(text).getAsJsonArray();
            if(array.size()<GuardPoseSettings.COUNT-1||array.size()>MAX_POSES)return null;
            var retained=new ArrayList<Pose>();
            for(var entry:array){Pose p=readPose(entry.toString());if(p==null)return null;retained.add(p);}
            // Restore the fifth built-in when reading a policy saved by the four-preset build.
            if(retained.stream().filter(p->p.preset).count()==4&&retained.stream().noneMatch(p->"preset5".equals(p.id)))retained.add(preset(4));
            Pose[] poses=retained.toArray(Pose[]::new);
            if(poses.length<GuardPoseSettings.COUNT||poses.length>MAX_POSES)return null;
            var ids=new HashSet<String>();int presets=0;
            for(Pose p:poses){if(p.otherHandItems==null)p.otherHandItems=new ArrayList<>();if(p.weaponItems==null)p.weaponItems=new ArrayList<>();if(!valid(p)||!ids.add(p.id)||p.preset!=p.id.matches("preset[1-5]"))return null;if(p.preset)presets++;}
            if(presets!=GuardPoseSettings.COUNT)return null;
            return Arrays.stream(poses).sorted(Comparator.comparing((Pose p)->!p.preset).thenComparing(p->p.id)).map(Pose::copy).toList();
        }catch(RuntimeException error){return null;}
    }

    private static String enforcedAnimations;
    public static synchronized String enforcedAnimations(){
        if(enforcedAnimations==null){Path path=FMLPaths.CONFIGDIR.get().resolve("mallard_guard/enforce/punchy_animations.json");try{enforcedAnimations=Files.isRegularFile(path)&&Files.size(path)<=MAX_ANIMATIONS_BYTES?Files.readString(path):"";if(parseAnimations(enforcedAnimations)==null)enforcedAnimations="";}catch(IOException e){enforcedAnimations="";}}
        return enforcedAnimations;
    }
    public static synchronized boolean saveEnforcedAnimations(String data){
        if(!data.isEmpty()&&parseAnimations(data)==null)return false;
        try{Path path=FMLPaths.CONFIGDIR.get().resolve("mallard_guard/enforce/punchy_animations.json");GuardConfig.preserveServerFile(path);Files.createDirectories(path.getParent());Path temporary=path.resolveSibling("punchy_animations.tmp");Files.writeString(temporary,data,StandardCharsets.UTF_8);try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}enforcedAnimations=data;return true;}catch(IOException e){return false;}
    }
    public static synchronized void clearEnforcedCache(){enforcedAnimations=null;}
}
