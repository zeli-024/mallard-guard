package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardControlSettings.Control;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardSparkTracerConfig;

import dev.zeli.mallardguard.GuardPoseSettings;

/** Page content uses the screen's shared controls and editable draft. */
final class GuardConfigPages {
    private GuardConfigPages() {}
    static void build(GuardConfigScreen screen) {
        if (!screen.settingsSearch.isBlank()) screen.buildSearchResults();
        else if(screen.clientSection&&screen.page==2)screen.buildFeedback(false);
        else if(screen.experimentalSection&&screen.page==24)screen.buildHighlight(false);
        else if (screen.enforceSection) {
            screen.toggle("Enforce client settings", () -> screen.configDraft.enforceEnabled, v -> {
                screen.configDraft.enforceEnabled = v;
                if (!v) screen.enforceHudOpen = screen.enforceAudioOpen = screen.enforcePunchyOpen = false;
                else screen.selectRememberedGroup();
            }, true, "Apply these starting values to connected clients and lock categories without a Don't Enforce exception.");
            screen.pageControls(screen.configDraft.enforceEnabled);
            screen.buildEnforcedDefaults();
        } else if (!screen.clientSection) {
            if(screen.page==18) screen.buildDiagnostics();
            if(screen.page==0)buildParrying(screen);
            if (screen.page == 6) screen.buildDamageTypes();

            if (screen.page == 12) {
                screen.toggle(Control.MOB_GUARD, "Mob guarding",()->screen.configDraft.mobGuard,v->{screen.configDraft.mobGuard=v;screen.dirtyMobs=true;},true,
                    "Let whitelisted mobs guard with eligible weapons or shields.\nEmpty hands and ranged weapons cannot guard. Default: Off.");
                screen.pageControls(screen.configDraft.mobGuard);
                screen.sliderBound(Control.MOB_DIFFICULTY, "Mob guard difficulty",()->screen.configDraft.mobDifficulty,"%",1,true,v->{screen.configDraft.mobDifficulty=v;screen.dirtyMobs=true;},
                    "Scale chance-based guard, rush and repeat-guard decisions.\n100% uses normal chances; 50% halves them; 0% stops new decisions.");
                screen.dropdown("Parry Colors",()->screen.mobColorsOpen,"Choose each mob particle family’s colors.\nThe impact uses the tracer’s Start/Middle/End colors.");
                if(screen.mobColorsOpen){
                    screen.colorField(Control.MOB_FLYING_COLOR, "Mob Flying Sparks Color",()->screen.configDraft.mobFlyingColor,v->screen.configDraft.mobFlyingColor=v,true,"Base color of mob Flying Sparks.\nDefault: #FFFFFF.");
                    screen.colorField(Control.MOB_DEBRIS_COLOR, "Mob Spark Debris Color",()->screen.configDraft.mobDebrisColor,v->screen.configDraft.mobDebrisColor=v,true,"Color of mob Spark Debris.\nDefault: #FFFFFF.");
                    screen.colorField(Control.MOB_TRACER_START, "Mob Tracer Start Color",()->screen.configDraft.mobTracerStartColor,v->screen.configDraft.mobTracerStartColor=v,true,"Color at spawn. Also colors the start of the mob impact.\nDefault: #FFFFFF.");
                    screen.colorField(Control.MOB_TRACER_MIDDLE, "Mob Tracer Middle Color",()->screen.configDraft.mobTracerMiddleColor,v->screen.configDraft.mobTracerMiddleColor=v,true,"Color halfway through fading. Also colors the middle of the impact.\nDefault: #FFFFFF.");
                    screen.colorField(Control.MOB_TRACER_END, "Mob Tracer End Color",()->screen.configDraft.mobTracerEndColor,v->screen.configDraft.mobTracerEndColor=v,true,"Color near disappearance. Also colors the end of the impact.\nDefault: #FFFFFF.");
                }
                screen.dropdown("Spawn Equipment",()->screen.mobEquipmentOpen,"Assign equipment for fresh whitelisted vanilla mobs.\nDifficulty does not affect gear chance.");
                if(screen.mobEquipmentOpen){
                    screen.sliderBound(Control.MOB_GEAR_CHANCE, "Mob gear chance",()->screen.configDraft.mobGearChance,"%",1,true,v->{screen.configDraft.mobGearChance=v;screen.dirtyMobs=true;},"Chance to generate assigned gear on a fresh mob.\nExisting melee gear and occupied armor slots are preserved. Default: 50%.");
                    screen.mobGearAssignments();
                }
                screen.dropdown("Mob Eligibility",()->screen.mobEligibilityOpen,"Choose which mobs may guard or receive generated gear. Bosses remain excluded.");
                if(screen.mobEligibilityOpen)screen.editBox(Control.MOB_WHITELIST, "Mob Guard Whitelist",screen.configDraft.mobWhitelist,v->{screen.configDraft.mobWhitelist=v;screen.dirtyMobs=true;},"Exact entity IDs, separated by commas; empty allows none.\nOnly listed mobs can guard or receive gear. Bosses remain excluded.");
            }
            if (screen.page == 11) {
                screen.dropdown("Shield Options", () -> screen.shieldTimingOpen, "Show shield parry timing and guard break settings.");
                if (screen.shieldTimingOpen) {
                screen.sliderBound(Control.SHIELD_PARRY_PUSHBACK_PERCENT,"Attacker Knockback",()->screen.configDraft.shieldRegularPush,"%",1,true,v->{screen.configDraft.shieldRegularPush=v;screen.dirtyShield=true;},"Attacker push on regular shield parries. Perfect shield parries use the separate area pushback.");
                screen.sliderBound(Control.SHIELD_PERFECT_TICKS, "Shield perfect window", () -> screen.configDraft.shieldPerfect, " ticks", 1, true, v -> {
                    screen.configDraft.shieldPerfect = Math.min(v, screen.configDraft.shieldWindow); screen.dirtyShield = true;
                }, "Perfect parry timing for shields. It must fit within the total shield parry window. 0 disables shield perfect parries. Default: 6 ticks.");
                screen.sliderBound(Control.SHIELD_PARRY_TICKS, "Shield parry window", () -> screen.configDraft.shieldWindow, " ticks", 1, true, v -> {
                    screen.configDraft.shieldWindow = v; screen.configDraft.shieldPerfect = Math.min(screen.configDraft.shieldPerfect, v); screen.dirtyShield = true;
                }, "Total shield parry time, including its perfect window. Default: 9 ticks (6 perfect, then 3 regular). Lowering this also limits perfect timing. Shield use continues into blocking until release or guard break.");
                screen.sliderBound(Control.SHIELD_RECHARGE_TICKS, "Shield parry recharge", () -> screen.configDraft.shieldRechargeTicks, " ticks", 1, true, v -> { screen.configDraft.shieldRechargeTicks = v; screen.dirtyShield = true; }, "Time before another shield parry after releasing guard. Regular shield parries halve it; perfect shield parries reset it. Separate from the guard break penalty.");
                screen.sliderBound(Control.SHIELD_MAX_BLOCKS, "Shield blocks before break", () -> screen.configDraft.shieldMaxBlocks, "", 1, true, v -> { screen.configDraft.shieldMaxBlocks = v; screen.dirtyShield = true; }, "Successful shield blocks allowed before the guard ends. 0 allows unlimited blocks. A disabled shield still follows vanilla's cooldown.");
                }
                screen.featureDivider("Shield Perfect Parry", () -> screen.shieldPerfectOpen,()->screen.configDraft.shieldConeEnabled,value->{screen.configDraft.shieldConeEnabled=value;screen.dirtyShield=true;},true,"Enable perfect-parry area damage, pushback and stun. Off preserves your settings.");
                if (screen.shieldPerfectOpen) {
                screen.sliderBound(Control.SHIELD_CONE_DEGREES, "Cone half-angle", () -> screen.configDraft.shieldCone, "°", 1, true, v -> { screen.configDraft.shieldCone = v; screen.dirtyShield = true; }, "Angle to each side of where you look. 90° covers the whole front half (180° total); 0° disables cone retaliation, stun and pushback.");
                screen.sliderBound(Control.SHIELD_CONE_REACH, "Cone reach", () -> screen.configDraft.shieldReach, " blocks", 1, true, v -> { screen.configDraft.shieldReach = v; screen.dirtyShield = true; }, "Maximum distance between your eyes and a target's eyes for perfect shield parry cone effects.");
                screen.sliderBound(Control.SHIELD_RETALIATION_PERCENT, "Shield retaliation", () -> screen.configDraft.shieldReturn, "%", 1, true, v -> { screen.configDraft.shieldReturn = v; screen.dirtyShield = true; }, "Percentage of the parried hit dealt to each target in the cone. 0% disables damage but allows configured stun and pushback.");
                screen.sliderBound(Control.SHIELD_STUN_TICKS, "Stun duration", () -> screen.configDraft.shieldStun, " ticks", 1, true, v -> { screen.configDraft.shieldStun = v; screen.dirtyShield = true; }, "Stop voluntary movement and attacks after cone pushback. 20 ticks = 1 second; 0 disables stun. Unlisted bosses are immune.");
                screen.sliderBound(Control.SHIELD_PUSHBACK_PERCENT, "Cone pushback", () -> screen.configDraft.shieldPerfectPush, "%", 1, true, v -> { screen.configDraft.shieldPerfectPush = v; screen.dirtyShield = true; }, "Pushes targets in the perfect parry cone away. 0% disables pushback.");
                }
                screen.dropdown("Additional Shields and Bosses", () -> screen.shieldIdsOpen, "Show extra shield item IDs and bosses eligible for stun.");
                if (screen.shieldIdsOpen) {
                screen.itemRuleAssignment("Shield Item Whitelist", screen.configDraft.shieldItems, v -> { screen.configDraft.shieldItems = v; screen.configDraft.invalidItemRules = false; screen.dirtyShield = true; },
                    "Assign item IDs, #tags, @mod namespaces, or ID/tag keywords to recognize extra shields. Default: Empty.");
                screen.itemRuleAssignment("Shield Item Blacklist", screen.configDraft.shieldBlacklist, v -> {screen.configDraft.shieldBlacklist=v;screen.configDraft.invalidItemRules=false;screen.dirtyShield=true;}, "Assigned items and matching rules excluded from shield recognition. Overrides tags and whitelist. Default: Empty.");
                screen.editBox(Control.SHIELD_STUN_BOSSES, "Stunnable bosses", screen.configDraft.stunnableBosses, v -> { screen.configDraft.stunnableBosses = v; screen.configDraft.invalidItemRules = false; screen.dirtyShield = true; },
                    "Comma-separated entity IDs or #entity tags. Listed bosses may be stunned by a shield perfect parry. Other bosses stay immune. Default: Empty.");
                }
            }
            if(screen.page==1)buildBlocking(screen);
            if (screen.page == 2) {
            screen.toggle(Control.HIT_PARTICLES, "Allow World Parry Effects", () -> screen.configDraft.hitParticles, v -> screen.configDraft.hitParticles = v, true,
                "Allow nearby players to see parry sparks and flashes. Blocks do not create particles.");
            screen.toggle(Control.HIT_SOUNDS, "Hit sounds", () -> screen.configDraft.serverHitSounds, v -> screen.configDraft.serverHitSounds = v, true,
                "Allow parry and block sounds to play for nearby players. Everyone controls their own playback volume.");
            }
            if (screen.page == 4) {
            screen.dropdown("Valid Parry Conditions", () -> screen.eligibilityOpen, "Choose eligible items and guard restrictions.");
            if (screen.eligibilityOpen) {
                screen.toggle(Control.ALLOW_EMPTY_HAND, "Empty Hand", () -> screen.configDraft.allowEmptyHand, v -> {screen.configDraft.allowEmptyHand=v;screen.dirtyShield=true;}, true,
                    "Allow empty-hand guarding only when BOTH hands are completely empty. Held items must qualify separately. Default: Off.");
                screen.choice("Tool Requirement", new String[]{"Tools Without Use", "All Tools", "Any Held Item"}, () -> screen.configDraft.allowAnyItem?2:screen.configDraft.allowUsableItems?1:0,
                    v -> {screen.configDraft.allowAnyItem=v==2;screen.configDraft.allowUsableItems=v==1;}, "Tools Without Use excludes tools with use actions. All Tools includes them. Any Held Item removes the tool requirement. Shields/whitelist also qualify; empty hands use their own toggle.");
                screen.blockCountAssignments();
                screen.toggle(Control.COOLDOWN_PREVENTS_GUARD, "Respect Item Cooldown", () -> screen.configDraft.cooldownPreventsGuard, v -> {screen.configDraft.cooldownPreventsGuard=v;screen.dirtyShield=true;}, true,
                    "Items on cooldown cannot guard. Disabled shields always obey cooldown. Default: On.");
                screen.toggle(Control.CONSUMABLE_PRIORITY, "Consumables Take Priority", () -> screen.configDraft.consumablePriority, v -> {screen.configDraft.consumablePriority=v;screen.dirtyShield=true;}, true,
                    "Eat or drink before guarding when either hand holds a consumable. Default: On.");
            }
            screen.dropdown("Priority", () -> screen.priorityOpen, "Choose which eligible hand guards.");
            if (screen.priorityOpen) {
                screen.choice("Parry Hand Priority", new String[]{"Random", "Off-Hand", "Main-Hand"}, () -> screen.configDraft.parryHandPriority,
                    v -> {screen.configDraft.parryHandPriority=v;screen.dirtyShield=true;}, "Choose the guarding item when both hands qualify. Random never repeats a hand more than twice. Default: Random.");
                screen.choice("Shield Parry Priority", new String[]{"Random", "Prefer Shield", "Prefer Main Hand"}, () -> screen.configDraft.shieldParryPriority,
                    v -> {screen.configDraft.shieldParryPriority=v;screen.dirtyShield=true;}, "Random chooses either eligible hand; Shield prefers shields. Prefer Main Hand prefers the main hand. Crouch override takes priority.");
                screen.toggle(Control.FORCE_CROUCH_OFFHAND, "Crouch Prefers Offhand", () -> screen.configDraft.forceCrouchOffhand, v -> {screen.configDraft.forceCrouchOffhand=v;screen.dirtyShield=true;}, true,
                    "Crouching selects an eligible offhand, overriding both priorities. Otherwise normal selection applies. Default: On.");
            }
            screen.dropdown("Item Durability Wear", () -> screen.durabilityOpen, "Durability consumed by guarded hits.");
            if (screen.durabilityOpen) {
                screen.sliderBound(Control.PARRY_WEAR, "Parry durability", () -> screen.configDraft.parryWear, "%", 10, true, v->screen.configDraft.parryWear=v, "Maximum item durability consumed per regular parry, rounded up. 0% disables wear.");
                screen.sliderBound(Control.PERFECT_WEAR, "Perfect durability", () -> screen.configDraft.perfectWear, "%", 10, true, v->screen.configDraft.perfectWear=v, "Maximum item durability consumed per perfect parry, rounded up. 0% disables wear.");
                screen.sliderBound(Control.BLOCK_WEAR, "Block durability", () -> screen.configDraft.blockWear, "%", 10, true, v->screen.configDraft.blockWear=v, "Maximum item durability consumed per block, rounded up. Shields use vanilla wear; items without durability are unaffected.");
            }
            }
        } else {
            if (screen.page == 7) buildWorldParticles(screen,false);
            if (screen.page == 17) {
            screen.featureDivider("Shield Icon", () -> screen.shieldOpen, () -> screen.configDraft.hud, value -> screen.configDraft.hud=value, false, "Show or hide shield icon settings.");
            if (screen.shieldOpen && screen.configDraft.hud) {
                screen.buildIconPosition(false);
                if (screen.configDraft.hud) screen.toggle(Control.SHIELD_EFFECTS, "Shield reactions", () -> screen.configDraft.shieldEffects, v -> screen.configDraft.shieldEffects = v, false,
                    "Enable the individual shield reactions below. Each reaction has its own controls. Default: On.");
                screen.buildShieldReactions(false);
                        }
            }
            if (screen.page == 15) {
                screen.dropdown("Hand Animation", () -> screen.animationOpen, "Show or hide first-person and third-person guard pose settings.");
                if (screen.animationOpen) {
                    screen.toggle(Control.THIRD_PERSON_ANIMATION, "Third-person guard pose", () -> screen.configDraft.thirdPersonAnimation, v -> screen.configDraft.thirdPersonAnimation = v, false,
                        "Show the guarding pose on players and supported humanoid mobs. Off hides it only on your client.");
                    screen.toggle(Control.FIRST_PERSON_ANIMATION, "First-person guard pose", () -> screen.simplePoseDraftEnabled(),
                    v -> screen.configDraft.firstPersonAnimation = v, false,
                    "Animate weapon guarding with the simple pose. Used as fallback when Punchy is unavailable; compatible Punchy takes priority. Shields animate separately.");
                }
            }
            if (screen.page == 3) {
            screen.sliderBound(Control.MASTER_VOLUME, "Master volume", () -> screen.configDraft.masterVolume, "%", 1, false, v -> screen.configDraft.masterVolume = v, "Scale all Mallard Guard sounds you hear. 0% mutes them; above 100% uses louder mixes where available.");
            screen.sliderBound(Control.PERFECT_VOLUME, "Perfect volume", () -> screen.configDraft.perfectVolume, "%", 1, false, v -> screen.configDraft.perfectVolume = v, "Perfect-parry and associated meme sound volume, multiplied by Master Volume.");
            screen.sliderBound(Control.PARRY_VOLUME, "Parry volume", () -> screen.configDraft.parryVolume, "%", 1, false, v -> screen.configDraft.parryVolume = v, "Regular-parry and associated meme sound volume, multiplied by Master Volume.");
            screen.sliderBound(Control.BLOCK_VOLUME, "Block volume", () -> screen.configDraft.blockVolume, "%", 1, false, v -> screen.configDraft.blockVolume = v, "Block and shield-parry sound volume, multiplied by Master Volume.");
            }
        }

        if (screen.experimentalSection && screen.page == 14) {
            if (PunchyGuardCompat.installed()) {

            screen.toggle("Guard Animations", () -> screen.configDraft.extraClient[65] != 0, v -> {screen.configDraft.extraClient[65]=v?1:0;screen.poseDraft.enabled=v;}, false, "Use Punchy for guard animations when its animation API is compatible, including newer versions. Automatically fall back to the simple pose if integration is unavailable. Default: On.");
            screen.pageControls(screen.configDraft.extraClient[65]!=0);
            screen.sliderBound(Control.PUNCHY_RELEASE_DELAY, "After Hitlag Hold", () -> screen.configDraft.extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT], " ms", 1, false, v -> screen.configDraft.extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT]=v, "Extra hold after hitlag finishes. No extra delay when hitlag does not occur. Applies to every Punchy preset. 0 removes the delay. Default: 50 ms.");
            screen.poseEditor(false);
            }
        }

    }
    private static void buildBlocking(GuardConfigScreen screen){
        var d=screen.configDraft;
        screen.featureDivider("Guard Stance",()->screen.guardOpen,()->d.block,v->d.block=v,true,"Keep guarding past the parry window. Off lowers guard after a parry and disables held blocking.");
        if(screen.guardOpen){
            screen.sliderBound(Control.BLOCK_REDUCTION,"Damage reduction",()->d.reductionPercent,"%",1,true,v->d.reductionPercent=v,"Damage prevented by held block. 50% halves it; 100% prevents it.");
            screen.sliderBound(Control.TOOL_MAX_BLOCKS,"Weapon blocks before break",()->d.toolMaxBlocks,"",1,true,v->{d.toolMaxBlocks=v;screen.dirtyShield=true;},"Successful weapon or empty-hand blocks before guard ends. 0 allows unlimited blocks.");
            screen.sliderBound(Control.SHIELD_BREAK_TICKS,"Guard break recharge",()->d.shieldBreakTicks," ticks",1,true,v->{d.shieldBreakTicks=v;screen.dirtyShield=true;},"Wait after the last allowed block before guarding again. Minimum matches normal recharge; maximum 10 seconds.");
            screen.sliderBound(Control.GUARD_MOVEMENT_PERCENT,"Guard movement",()->d.guardMovementPercent,"%",1,true,v->d.guardMovementPercent=v,"Movement speed while guarding, including parry and block. 100% means normal speed.");
        }
        screen.pageControls(d.block);
        screen.dropdown("Projectiles",()->screen.blockProjectileOpen,"Projectile deflection and damage reduction while blocking.");
        if(screen.blockProjectileOpen){
            screen.sliderBound(Control.BLOCK_DEFLECT_CHANCE,"Block deflect chance",()->d.blockDeflectChance,"%",1,true,v->d.blockDeflectChance=v,"Chance to deflect a projectile in a random direction. 0% disables deflection.");
            screen.toggle(Control.BLOCK_PROJECTILES,"Block projectiles",()->d.blockProjectiles,v->d.blockProjectiles=v,true,"Reduce projectile damage when a projectile is not deflected.");
        }
        screen.dropdown("Explosions",()->screen.blockExplosionOpen,"Explosion damage reduction while blocking.");
        if(screen.blockExplosionOpen)screen.toggle(Control.BLOCK_EXPLOSIONS,"Block explosions",()->d.blockExplosions,v->d.blockExplosions=v,true,"Reduce explosion damage using the held-block damage reduction.");
    }
    static void buildWorldParticles(GuardConfigScreen screen,boolean server){
        int[] target=server?screen.configDraft.enforcedDefaults:screen.configDraft.extraClient;
        screen.featureDivider("Flying Sparks", () -> screen.animatedOpen, () -> target[GuardParticleColors.FLYING_ENABLED]!=0, value -> target[GuardParticleColors.FLYING_ENABLED]=value?1:0, server, "Uneven bursts with animated firework sparks and occasional electric sparks. On/Off preserves your intensity settings.");
        if(screen.animatedOpen) {
            screen.sliderBound(Control.FLYING_BASE_INTENSITY, "Base Intensity", () -> target[16], "%", 1, server, v -> target[16]=v, "Regular-parry amount. 100% starts with 100 sparks; 0% turns Flying Sparks off completely. Both appearances share the total.");
            screen.sliderBound(Control.FLYING_PERFECT_INTENSITY, "Perfect Intensity", () -> target[17], "%", 1, server, v -> target[17]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
            screen.sliderBound(Control.FLYING_DAMAGE_SCALING, "Damage Intensity", () -> target[18], "%", 1, server, v -> target[18]=v, "Increase the amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All particles share a 512-head burst cap.");
            screen.particleAppearance(server,GuardParticleColors.BASE,GuardParticleColors.FLYING_SIZE_VARIANCE+1);
        }

        screen.featureDivider("Spark Debris", () -> screen.dotStreaksOpen, () -> target[GuardParticleColors.DEBRIS_ENABLED]!=0, value -> target[GuardParticleColors.DEBRIS_ENABLED]=value?1:0, server, "Small debris sparks with immediate gravity, brief dotted tails, and diminishing bounces. On/Off preserves your settings.");
        if(screen.dotStreaksOpen) {
            screen.sliderBound(Control.DEBRIS_BASE_INTENSITY, "Debris Base Intensity", () -> target[19], "%", 1, server, v -> target[19]=v, "Regular-parry amount. 100% starts with 100 debris sparks; 0% turns Spark Debris off completely.");
            screen.sliderBound(Control.DEBRIS_PERFECT_INTENSITY, "Debris Perfect Intensity", () -> target[20], "%", 1, server, v -> target[20]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
            screen.sliderBound(Control.DEBRIS_DAMAGE_SCALING, "Debris Damage Intensity", () -> target[21], "%", 1, server, v -> target[21]=v, "Increase debris amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All three effects share the head budget.");
            screen.particleAppearance(server,GuardParticleColors.DEBRIS_COLOR,GuardParticleColors.DEBRIS_SIZE+1);
        }

        int tracerOffset=GuardSparkTracerConfig.OFFSET;
        screen.featureDivider("Spark Tracers", () -> screen.sparkTracersOpen, () -> target[GuardParticleColors.TRACER_ENABLED]!=0, value -> target[GuardParticleColors.TRACER_ENABLED]=value?1:0, server, "Upright tick-by-tick square trails. On/Off preserves your settings.");
        if(screen.sparkTracersOpen) {
            for(int k:GuardSparkTracerConfig.DISPLAY_ORDER) {
                final int index=tracerOffset+k;
                screen.slider(GuardSparkTracerConfig.LABELS[k],()->target[index],0,100,"%",1,server,v->target[index]=v,GuardSparkTracerConfig.TIPS[k]);
            }
            screen.particleAppearance(server,GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.PREVIOUS_LENGTH);
        }
        screen.featureDivider("Center Impact", () -> screen.centerImpactOpen, () -> target[GuardParticleColors.IMPACT_ENABLED]!=0, value -> target[GuardParticleColors.IMPACT_ENABLED]=value?1:0, server, "Six sharp frames disperse into fading arcs. Uses tracer colors. On/Off preserves your settings.");
        if(screen.centerImpactOpen)screen.particleAppearance(server,GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_DURATION+2);

    }
    private static void buildParrying(GuardConfigScreen screen){
        var d=screen.configDraft;
        screen.featureDivider("Parrying",()->screen.timingOpen,()->d.parry,v->d.parry=v,true,"Timed parries and their windows.");
        if(screen.timingOpen&&d.parry){
            screen.sliderBound(Control.PERFECT_TICKS,"Perfect Window",()->d.perfect," ticks",1,true,v->d.perfect=Math.min(v,d.window),"Perfect timing at the start of the total parry window. 0 disables perfect parries. Default: 6 ticks.");
            screen.sliderBound(Control.PARRY_TICKS,"Parry Window",()->d.window," ticks",1,true,v->{d.window=v;d.perfect=Math.min(d.perfect,v);},"Total time to catch a hit, including perfect timing. Default: 9 ticks (6 perfect, then 3 regular).");
            screen.sliderBound(Control.RECHARGE_TICKS,"Recharge",()->d.recharge," ticks",1,true,v->d.recharge=v,"Wait before another parry attempt.");
            screen.sliderBound(Control.FACING_ANGLE,"Facing Angle",()->d.angle,"°",1,true,v->d.angle=v,"180° covers the front half; 360° covers all directions.");
            screen.sliderBound(Control.FOLLOW_UP_TICKS,"Follow-up Parries",()->d.followUp," ticks",1,true,v->d.followUp=v,"Catch additional valid hits after success. These hits do not extend the timer. Default: 5 ticks.");
        }
        screen.featureDivider("Parried",()->screen.whenParriedOpen,()->d.parriedEnabled,v->{d.parriedEnabled=v;if(v)d.incomingReaction=Math.max(1,d.incomingReaction);screen.dirtyShield=true;},true,"Gameplay reaction when your attack is parried. Uses the shared client feedback settings.");
        if(screen.whenParriedOpen&&d.parriedEnabled){
            screen.parriedReaction();
            screen.controls(d.incomingReaction == 2 || d.incomingReaction == 3);
            screen.sliderBound(Control.PARRIED_GUARD_BREAK_TICKS,"Guard Break Duration",()->d.parriedGuardBreakTicks," ticks",1,true,v->{d.parriedGuardBreakTicks=v;screen.dirtyShield=true;},"Recharge penalty in Guard Break or Both mode. Default: 15 ticks; maximum: 60. 0 disables this penalty.");
            screen.controls(d.incomingReaction == 1 || d.incomingReaction == 3);
            screen.sliderBound(Control.PARRIED_STUN_TICKS,"Stun Duration",()->d.parriedStunTicks," ticks",1,true,v->{d.parriedStunTicks=v;screen.dirtyShield=true;},"Movement, attack and guard restriction in Stun or Both mode. Default: 15 ticks; maximum: 60. 0 disables stun.");
            screen.controls(true);
        }
        screen.dropdown("Health",()->screen.healingOpen,"Healing and damage returned on parries.");
        if(screen.healingOpen){
            screen.toggle(Control.PARRY_HEALING,"Parry Healing",()->d.parryHealing,v->d.parryHealing=v,true,"Heal once per successful attempt. Applies to players and guarding mobs. Default: Off.");
            screen.controls(d.parryHealing);
            screen.sliderBound(Control.PARRY_HEALING_HEARTS,"Parry Healing Amount",()->d.parryHealingHearts," hearts",1,true,v->d.parryHealingHearts=v,"Perfect parries heal this amount; regular parries heal half.");
            screen.controls(true);
            screen.toggle(Control.RETALIATION_ENABLED,"Parry Retaliation",()->d.retaliationEnabled,v->{d.retaliationEnabled=v;screen.dirtyShield=true;},true,"Return damage on parries. Off preserves both multipliers; knockback remains separate.");
            screen.controls(d.retaliationEnabled);
            screen.sliderBound(Control.PARRY_RETALIATION,"Regular Retaliation",()->d.parryReturnPercent,"%",1,true,v->d.parryReturnPercent=v,"Percentage of regular-parried damage returned to the attacker.");
            screen.sliderBound(Control.PERFECT_RETALIATION,"Perfect Retaliation",()->d.perfectReturnPercent,"%",1,true,v->d.perfectReturnPercent=v,"Percentage of perfect-parried damage returned to the attacker.");
        }
        screen.dropdown("Knockback",()->screen.pushbackOpen,"Weapon attacker push, defender recoil and perfect scaling.");
        if(screen.pushbackOpen){
            screen.sliderBound(Control.TOOL_PUSHBACK_PERCENT,"Attacker Knockback",()->d.toolPush,"%",1,true,v->{d.toolPush=v;screen.dirtyShield=true;},"Push the attacker on weapon parries. Shield attacker knockback is in Shield Options.");
            screen.sliderBound(Control.KNOCKBACK_STRENGTH,"Defender Knockback",()->d.knockbackStrength,"%",1,true,v->d.knockbackStrength=v,"Your recoil after a parry. Shields use half strength; 0 disables recoil.");
            screen.sliderBound(Control.PERFECT_KNOCKBACK_MULTIPLIER,"Perfect Multiplier",()->d.perfectKnockbackMultiplier,"%",1,true,v->{d.perfectKnockbackMultiplier=v;screen.dirtyShield=true;},"Scale perfect weapon attacker knockback and defender recoil. 100% retains normal strength.");
        }
        screen.dropdown("Parry Types",()->screen.fallOpen,"Projectile, explosion and fall parries. Falls always require downward aim.");
        if(screen.fallOpen){
            screen.toggle(Control.PARRY_PROJECTILES,"Projectile Parry",()->d.parryProjectiles,v->d.parryProjectiles=v,true,"Deflect projectile entities and prevent eligible projectile damage.");
            screen.controls(d.parryProjectiles);
            screen.toggle(Control.PERFECT_PROJECTILES_ONLY,"Perfect Only Projectiles",()->d.perfectProjectilesOnly,v->{d.perfectProjectilesOnly=v;screen.dirtyShield=true;},true,"Require perfect timing to parry projectiles. Blocking is unchanged.");
            screen.controls(true);
            screen.toggle(Control.PARRY_EXPLOSIONS,"Explosion Parry",()->d.parryExplosions,v->d.parryExplosions=v,true,"Prevent eligible explosion damage with a timed parry.");
            screen.controls(d.parryExplosions);
            screen.toggle(Control.PERFECT_EXPLOSIONS_ONLY,"Perfect Only Explosions",()->d.perfectExplosionsOnly,v->d.perfectExplosionsOnly=v,true,"Require perfect timing to parry explosions.");
            screen.controls(true);
            screen.toggle(Control.FALL_PARRY,"Fall Parry",()->d.fallParry,v->d.fallParry=v,true,"Parry falls while looking at least 40° downward. Also controls wall-impact parries.");
            screen.controls(d.fallParry);
            screen.toggle(Control.PERFECT_FALL_ONLY,"Perfect Only Falls",()->d.perfectFallOnly,v->{d.perfectFallOnly=v;screen.dirtyShield=true;},true,"Only perfect timing catches falls or wall impacts.");
            screen.sliderBound(Control.FALL_LAUNCH_POWER,"Fall Launch",()->d.fallLaunchPower,"%",1,true,v->d.fallLaunchPower=v,"Launch after a fall parry, carrying current horizontal momentum. Perfect parries launch farther.");
            screen.sliderBound(Control.FALL_BLAST_STRENGTH,"Fall Blast",()->d.fallBlastStrength,"%",1,true,v->d.fallBlastStrength=v,"Blast size after parrying a fall over ten blocks. 0 disables it.");
            screen.toggle(Control.FALL_BREAK_BLOCKS,"Fall Blast Breaks Blocks",()->d.fallBreakBlocks,v->d.fallBreakBlocks=v,true,"Allow the long-fall blast to break terrain.");
        }
    }

}
