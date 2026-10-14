package dev.zeli.mallardguard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Immutable navigation catalogue; searching never rebuilds the menu definitions. */
final class GuardConfigSearch {
    private GuardConfigSearch() {}
    record Entry(int section,int page,String category,String name,String group) {
        Entry(int section,int page,String category,String name){this(section,page,category,name,"");}
    }
    private static final List<Entry> ENTRIES = catalogue();
    private static void addSearchEntries(List<Entry> entries, int section, int page, String category, String names) {
        for (String name : names.split("\\|")) entries.add(new Entry(section, page, category, name));
    }

    private static List<Entry> catalogue() {
        List<Entry> all = new ArrayList<>();
        addSearchEntries(all,3,12,"Mob Guard","Mob guarding|Mob guard difficulty|Parry Colors|Mob Flying Sparks Color|Mob Spark Debris Color|Mob Tracer Start Color|Mob Tracer Middle Color|Mob Tracer End Color|Spawn Equipment|Mob gear chance|Mob Gear Assignments|Mob Eligibility|Mob Guard Whitelist");
        addSearchEntries(all, 0, 7, "World Particles", "Shield Forward Bias|Flying Sparks|Base Intensity|Perfect Intensity|Damage Intensity|"+String.join("|",new String[]{"Flying Size","Flying Size Variance","Flying Lifetime","Flying Base Color"})
            +"|Spark Debris|Debris Base Intensity|Debris Perfect Intensity|Debris Damage Intensity|"+String.join("|",new String[]{"Debris Size","Debris Lifetime","Debris Base Color"})
            +"|Spark Tracers|Tracer Base Intensity|Tracer Perfect Intensity|Tracer Damage Intensity|"+String.join("|",new String[]{"Tracer Size","Tracer Lifetime","Tracer Tail Length","Tracer Start Color","Tracer Middle Color","Tracer End Color"})
            +"|Center Impact|Impact Size|Impact Spin Speed|Impact Opacity|Impact Duration");
        addSearchEntries(all,1,0,"Parrying","Parrying|Perfect Window|Parry Window|Recharge|Facing Angle|Follow-up Parries|Parried|Reaction|Guard Break Duration|Stun Duration|Health|Parry Healing|Parry Healing Amount|Parry Retaliation|Regular Retaliation|Perfect Retaliation|Knockback|Attacker Knockback|Defender Knockback|Perfect Multiplier|Parry Types|Projectile Parry|Perfect Only Projectiles|Explosion Parry|Perfect Only Explosions|Fall Parry|Perfect Only Falls|Fall Launch|Fall Blast|Fall Blast Breaks Blocks");
        all.add(new Entry(0,2,"Screen Effects / Hitlag","Hitlag","Hitlag"));
        all.add(new Entry(0,2,"Screen Effects / Hitlag","Perfect Only","Hitlag"));
        all.add(new Entry(0,2,"Screen Effects / Hitlag","Hitlag Duration","Hitlag"));
        all.add(new Entry(0,2,"Screen Effects / Hitlag","Regular Duration","Hitlag"));
        all.add(new Entry(0,2,"Screen Effects / Impact Frame","Impact Frame","Impact Frame"));
        all.add(new Entry(0,2,"Screen Effects / Impact Frame","Preview","Impact Frame"));
        all.add(new Entry(0,2,"Screen Effects / Impact Frame","Perfect Only","Impact Frame"));
        all.add(new Entry(3,24,"Target Highlight","Target Highlight","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Perfect Only","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Attacker Highlight","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Attacker Highlight Color","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Target Highlight Color","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Timing","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Duration","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","World Dim","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Fade In","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Fade Out","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Glow Strength","Target Highlight"));
        all.add(new Entry(3,24,"Target Highlight","Preview","Target Highlight"));
        all.add(new Entry(0,2,"Screen Effects / White Flash","White Flash","White Flash"));
        all.add(new Entry(0,2,"Screen Effects / White Flash","Perfect Only","White Flash"));
        all.add(new Entry(0,2,"Screen Effects / White Flash","Perfect Intensity","White Flash"));
        all.add(new Entry(0,2,"Screen Effects / White Flash","Regular Intensity","White Flash"));
        all.add(new Entry(0,2,"Screen Effects / White Flash","Meme Flash","White Flash"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Camera Shake","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Perfect Only","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Perfect Duration","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Perfect Strength","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Regular Duration","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Camera Shake","Regular Strength","Camera Shake"));
        all.add(new Entry(0,2,"Screen Effects / Chromatic Aberration","Chromatic Aberration","Chromatic Aberration"));
        all.add(new Entry(0,2,"Screen Effects / Chromatic Aberration","Perfect Only","Chromatic Aberration"));
        all.add(new Entry(0,2,"Screen Effects / Chromatic Aberration","Perfect Duration","Chromatic Aberration"));
        all.add(new Entry(0,2,"Screen Effects / Chromatic Aberration","Regular Duration","Chromatic Aberration"));
        all.add(new Entry(0,2,"Screen Effects / Chromatic Aberration","Include HUD","Chromatic Aberration"));
        addSearchEntries(all,0,17,"HUD Icons","Shield Icon|Shield icon|Icon Position|Shield reactions|Regular Parry|Perfect Parry|Blocked Hit|Guard Broken|Parry Ready");
        for(int r=0;r<dev.zeli.mallardguard.GuardShieldReactions.NAMES.length;r++){
            String reaction=dev.zeli.mallardguard.GuardShieldReactions.NAMES[r];
            for(int c=1;c<=5;c++)if(dev.zeli.mallardguard.GuardShieldReactions.used(r*dev.zeli.mallardguard.GuardShieldReactions.STRIDE+c)){
                String part=c==1&&r==4?"Lift":c==3?"Highlight":dev.zeli.mallardguard.GuardShieldReactions.CONTROLS[c];
        addSearchEntries(all,0,17,"HUD Icons",reaction+" "+part+(c<5?"|"+reaction+" "+part+" Strength":""));
            }
        }
        addSearchEntries(all,1,6,"Damage Sources","Damage Types|Recent Hits|Runtime Damage Types");
        all.add(new Entry(1,18,"Diagnostics","Diagnostic Logging","Combat"));
        for(var channel:dev.zeli.mallardguard.GuardDiagnostics.channels())all.add(new Entry(1,18,"Diagnostics",channel.name(),channel.category()));
        addSearchEntries(all, 0, 15, "Animations", "Hand Animation|Third-person guard pose|First-person guard pose");
        addSearchEntries(all, 0, 3, "Audio Sliders", "Master volume|Perfect volume|Parry volume|Block volume");
        addSearchEntries(all, 1, 1, "Blocking", "Guard Stance|Damage reduction|Weapon blocks before break|Guard break recharge|Guard movement|Projectiles|Block deflect chance|Block projectiles|Explosions|Block explosions");
        addSearchEntries(all, 1, 11, "Shields", "Shield Options|Attacker Knockback|Shield perfect window|Shield parry window|Shield parry recharge|Shield blocks before break|Shield Perfect Parry|Cone half-angle|Cone reach|Shield retaliation|Stun duration|Cone pushback|Additional Shields and Bosses|Shield Item Assignments|Stunnable bosses");
        addSearchEntries(all, 1, 6, "Damage Sources", "Common Damage Types|Drowning|Standing in fire|Burning|Lava|Hot floor|Campfire|Starvation|Wither|Poison / Instant Damage|Indirect magic|Fall damage|Wall collision|Vanilla /kill|Damage Types");
        addSearchEntries(all, 1, 2, "Multiplayer", "Allow World Parry Effects|Hit sounds");
        addSearchEntries(all, 1, 4, "Eligibility", "Valid Parry Conditions|Empty Hand|Tool Requirement|Item Assignments|Respect Item Cooldown|Consumables Take Priority|Priority|Parry Hand Priority|Shield Parry Priority|Crouch Prefers Offhand|Item Durability Wear|Parry durability|Perfect durability|Block durability");
        addSearchEntries(all, 3, 14, "Punchy", "Guard Animations|After Hitlag Hold|Pose Presets");
        addSearchEntries(all, 2, 8, "Enforce", "Enforce client settings|World Particles|Screen Effects|Shield Icon|Audio Sliders|Hand Animation|Experimental Punchy|Punchy guard poses|After Hitlag Hold|Pose Presets|Don't Enforce");
        for (Entry entry : List.copyOf(all)) if (entry.section() == 0||entry.section()==3&&entry.page()==24)
            all.add(new Entry(2,entry.page()==24?25:entry.page()==7?8:entry.page()==2?19:entry.page()==17?20:entry.page()==3?21:22,"Enforce "+entry.category(),entry.name(),entry.group()));
        return List.copyOf(all);
    }
    static List<Entry> results(String query) {
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return ENTRIES.stream().filter(entry -> {
            String match = (entry.category() + " " + entry.name()).toLowerCase(Locale.ROOT);
            for (String term : terms) if (!match.contains(term)) return false;
            return true;
        }).toList();
    }

}
