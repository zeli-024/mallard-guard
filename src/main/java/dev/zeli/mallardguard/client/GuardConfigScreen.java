package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfigTransfer;

import dev.zeli.mallardguard.GuardControlSettings;
import dev.zeli.mallardguard.GuardControlSettings.Control;
import dev.zeli.mallardguard.GuardParticleConfig;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardScreenSettings;
import dev.zeli.mallardguard.GuardSparkTracerConfig;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardShieldReactions;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardPoseSettings;
import dev.zeli.mallardguard.GuardDamageRules;
import dev.zeli.mallardguard.GuardClientPreset;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class GuardConfigScreen extends Screen {
    private final boolean operator;
    private final Screen parent;
    private static int rememberedClientPage = 7, rememberedServerPage, rememberedSection, rememberedEnforcePage = 8;
    private static int rememberedClientRow, rememberedServerRow, rememberedEnforceRow, rememberedExperimentalRow, rememberedExperimentalPage = 12;
    private static final Map<String, String> rememberedGroups = new HashMap<>();
    int page, enforcePage=rememberedEnforcePage;
    private int clientPage = rememberedClientPage, serverPage = rememberedServerPage, clientRow = rememberedClientRow, serverRow = rememberedServerRow, enforceRow = rememberedEnforceRow, experimentalRow = rememberedExperimentalRow, experimentalPage = rememberedExperimentalPage;
    private int sidebarOffset;
    boolean experimentalSection = rememberedSection == 3;
    boolean clientSection = rememberedSection == 0;
    boolean enforceSection = rememberedSection == 2, enforceHudOpen, enforceAudioOpen, enforcePunchyOpen;

    private int activeClientCategory;
    int layoutTop, panelLeft, panelWidth, firstRow, visibleRows, sidebarLeft, sidebarWidth, controlIndex;
    private boolean draggingScroll;
    private double scrollGrab;
    private boolean dirty;
    private boolean dirtyClient, dirtyServer, dirtyServerRules, dirtyPolicy;
    private boolean dirtyDamageRules;
    boolean dirtyShield, dirtyMobs;

    boolean mobEligibilityOpen, mobColorsOpen, mobEquipmentOpen;

    private SavedState appliedState;
    private static int nextSaveRequest;
    private PendingSave pendingSave;
    private boolean promptAfterSave;
    private record PendingSave(int request, int mask, SavedState submitted, long started, boolean clientSaved, boolean close) {}

    final GuardConfigDraft configDraft = new GuardConfigDraft();
    private record SavedState(java.util.Map<String,Object> fields, GuardPoseManagerScreen.Draft poses) {}
    private SavedState captureState() {
        return new SavedState(configDraft.capture(), poseDraft == null ? null : poseDraft.copy());
    }

    private void acceptReceivedValues(SavedState before) {
        if(appliedState==null)return;
        SavedState after=captureState();
        var baseline=new LinkedHashMap<>(appliedState.fields());
        for(var entry:after.fields().entrySet())if(!java.util.Objects.deepEquals(entry.getValue(),before.fields().get(entry.getKey())))baseline.put(entry.getKey(),entry.getValue());
        appliedState=new SavedState(baseline,appliedState.poses());
    }

    private void restoreField(String key, Object value) { configDraft.restore(key, value); }

    boolean applyExternalChanges() {
        save(false);
        if (pendingSave != null || dirty) { Minecraft.getInstance().setScreen(this); return false; }
        return true;
    }
    private void showAppliedPrompt() {
        rebuildWidgets();
        GuardUi.choices(this,"Changes applied. Stay on this screen?","",
            new GuardUi.Choice("Stay",()->Minecraft.getInstance().setScreen(this)),
            new GuardUi.Choice("Close",()->Minecraft.getInstance().setScreen(parent)));
    }
    private void applyPrompt() {
        promptAfterSave = true;
        save(false);
        if (pendingSave == null) {
            if (!dirty) showAppliedPrompt();
            promptAfterSave = false;
        }
    }

    private void acceptSavedValues(SavedState submitted, int requested, int accepted, boolean clientSaved) {
        var baseline = new LinkedHashMap<>(appliedState.fields());
        SavedState current = captureState();
        for (var entry : submitted.fields().entrySet()) {
            int groups = configDraft.serverGroups(entry.getKey());
            boolean saved = groups == 0 ? clientSaved : (groups & requested) != 0 && (groups & requested & ~accepted) == 0;
            if (saved) baseline.put(entry.getKey(), groups == 0 ? current.fields().get(entry.getKey()) : entry.getValue());
        }
        appliedState = new SavedState(baseline, clientSaved ? current.poses() : appliedState.poses());
    }
    public void saveResult(GuardPackets.SaveResult result) {
        PendingSave pending = pendingSave;
        if (pending == null || result.request() != pending.request()) return;
        pendingSave = null;
        int accepted = result.accepted() & pending.mask();
        acceptSavedValues(pending.submitted(), pending.mask(), accepted, pending.clientSaved());
        dirtyServerRules = (pending.mask() & GuardPackets.SAVE_RULES & ~accepted) != 0;
        dirtyMobs = (pending.mask() & GuardPackets.SAVE_MOBS & ~accepted) != 0;
        dirtyShield = (pending.mask() & GuardPackets.SAVE_SHIELD & ~accepted) != 0;
        dirtyDamageRules = (pending.mask() & GuardPackets.SAVE_DAMAGE & ~accepted) != 0;
        dirtyPolicy = (pending.mask() & GuardPackets.SAVE_POLICY & ~accepted) != 0;
        dirtyServer = dirtyServerRules || dirtyMobs || dirtyShield || dirtyDamageRules || dirtyPolicy;
        dirtyClient = false;
        dirty = hasChanges();
        if (accepted == pending.mask()) {
            GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the " + (pending.clientSaved() ? "Client and Server" : "Server") + "."));
            if (pending.close()) Minecraft.getInstance().setScreen(parent);
            else if (promptAfterSave) showAppliedPrompt();
            else rebuildWidgets();
        } else {
            if (pending.clientSaved()) GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the Client."));
            GuardClient.configMessage(Component.literal("Mallard Guard: " + (result.error().isEmpty() ? "The server rejected some settings. These changes remain unsaved." : result.error())));
            rebuildWidgets();
        }
        promptAfterSave = false;
    }
    @Override public void tick() {
        super.tick();
        if (pendingSave != null && (Minecraft.getInstance().getConnection() == null || System.nanoTime() - pendingSave.started() > 15_000_000_000L)) {
            saveResult(new GuardPackets.SaveResult(pendingSave.request(), 0, "The server did not confirm this save. Server changes remain unsaved; reconnect or try again."));
        }
    }
    private void confirmReset(boolean ignored) {
        GuardUi.choices(this,"Reset settings","Restore defaults. Resetting Punchy also clears custom presets and assignments. Apply to save the changes.",
            new GuardUi.Choice("Reset Current Page",()->{reset(false);Minecraft.getInstance().setScreen(this);}),
            new GuardUi.Choice("Reset All Settings",()->{resetAllSettings();Minecraft.getInstance().setScreen(this);}),
            new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
    }
    private void resetAllSettings(){
        configDraft.debugFlags=0;dirtyClient=dirty=true;
        for(int category:CLIENT_PAGES)if(!GuardClient.clientCategoryLocked(categoryMask(category)))resetClientPage(category);
        if(!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY))resetClientPage(14);
        if(!GuardClient.clientCategoryLocked(GuardClientPreset.SCREEN))resetClientPage(24);
        if(operator){
            var defaults=GuardConfig.defaultSnapshot(true);
            for(int category:SERVER_PAGES)resetServerPage(category,defaults);
            for(int category:EXPERIMENTAL_PAGES)resetServerPage(category,defaults);
            configDraft.enforceEnabled=false;configDraft.dontEnforceMask=0;configDraft.enforcedPoseData="";
            System.arraycopy(GuardClientPreset.DEFAULTS,0,configDraft.enforcedDefaults,0,configDraft.enforcedDefaults.length);
            dirtyServer=dirtyPolicy=dirty=true;
        }
        rebuildWidgets();
    }

    GuardPoseManagerScreen.Draft poseDraft;

    private final List<String> recentHits = new ArrayList<>();
    private final List<String> damageCatalog = new ArrayList<>();
    private EditBox settingsSearchBox;
    String settingsSearch = "";
    private boolean settingsSearchChanged;
    private final Map<String, Integer> visibleSettingRows = new HashMap<>();
    private final Map<String, String> sourceMods = new HashMap<>();
    boolean sparkTracersOpen, dotStreaksOpen, animatedOpen, shieldOpen, animationOpen;
    boolean healingOpen, centerImpactOpen;

    boolean timingOpen, pushbackOpen;
    boolean guardOpen, blockProjectileOpen, blockExplosionOpen;
    boolean eligibilityOpen, priorityOpen, durabilityOpen;
    boolean shieldTimingOpen, shieldPerfectOpen, shieldIdsOpen;
    private String reactionGroup="Parry";
    boolean commonDamageOpen, fallOpen;
    private String diagnosticGroup="Combat";
    private int totalRows;

    private final Map<Integer, String> changedRows = new HashMap<>();
    private final Map<Integer, String> originalRows = new HashMap<>();
    private final Map<Integer,Runnable> rowRestores=new HashMap<>();
    private final Map<Integer,Boolean> rowEnabled=new HashMap<>();
    // A page master owns nested dividers; each sibling feature starts from that parent.
    private boolean pageControlsEnabled=true,featureControlsEnabled=true;
    void pageControls(boolean enabled){pageControlsEnabled=featureControlsEnabled=enabled;}
    void controls(boolean enabled){featureControlsEnabled=pageControlsEnabled&&enabled;}
    private final Map<Integer,Button> rowUndoButtons=new HashMap<>();
    private record RowControl(AbstractWidget widget, int x, int width, boolean header) {}
    private final Map<Integer,RowControl> rowControls=new HashMap<>();
    private final Map<Integer, java.util.function.Supplier<String>> rowValues = new HashMap<>();
    private final List<DropdownLabel> dropdownLabels = new ArrayList<>();
    private final List<SettingLabel> sectionLabels = new ArrayList<>();
    private Button logoButton, reportButton;
    private final List<Button> themedButtons = new ArrayList<>();
    private final Map<Button, String> buttonTips = new HashMap<>();
    private final Map<AbstractWidget, String> widgetTips = new HashMap<>();
    private static final int WARM_EDGE = GuardUi.WARM;
    private static final int TEXT = GuardUi.TEXT;
    String feedbackGroup="Hitlag";
    boolean whenParriedOpen;
    private static final String[] FEEDBACK_GROUPS={"Hitlag","Impact Frame","White Flash","Camera Shake","Chromatic Aberration"};
    private static final int ROW_STEP = 25;
    private final List<SettingLabel> settingLabels = new ArrayList<>();
    private static final String[] SERVER_TABS = {"Parrying","Blocking","Eligibility","Shields","Damage Sources","Multiplayer","Diagnostics"};
    private static final int[] SERVER_PAGES = {0,1,4,11,6,2,18};
    private static final String[] CLIENT_TABS = {"World Particles", "Screen Effects", "HUD Icons", "Audio Sliders", "Animations"};
    private static final int[] CLIENT_PAGES = {7, 2, 17, 3, 15};
    private static final int[] EXPERIMENTAL_PAGES = {12, 14, 24};
    private static final String[] EXPERIMENTAL_TABS = {"Mob Guard", "Punchy", "Target Highlight"};
    private static final int[] ENFORCE_PAGES={8,19,20,21,22,23,25};
    private static final String[] ENFORCE_TABS={"World Particles","Screen Effects","HUD Icons","Audio Sliders","Animations","Punchy","Target Highlight"};
    private final java.util.Set<Button> redButtons = new java.util.HashSet<>();

    public GuardConfigScreen(GuardPackets.Settings settings, Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
        this.page = experimentalSection ? experimentalPage : enforceSection ? enforcePage : clientSection ? clientPage : serverPage;
        this.firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        operator = settings.operator();
        GuardPackets.ClientPolicy policy = GuardClient.currentPolicy();
        configDraft.enforcedPoseData=policy.animations();
        configDraft.enforceEnabled = policy.enabled();
        enforceHudOpen = enforceAudioOpen = false;
        configDraft.dontEnforceMask = policy.dontEnforceMask();
        int[] received = GuardClientPreset.parse(policy.defaults());
        configDraft.enforcedDefaults = received == null ? GuardClientPreset.DEFAULTS.clone() : received;
        configDraft.serverHitSounds = settings.hitSounds(); configDraft.serverMasterVolume = settings.masterVolume();
        configDraft.serverPerfectVolume = settings.perfectVolume(); configDraft.serverParryVolume = settings.parryVolume(); configDraft.serverBlockVolume = settings.blockVolume();
        configDraft.parry = settings.parry(); configDraft.block = settings.block(); configDraft.parryDrowningFire = settings.parryDrowningFire();
        configDraft.parryStarvation = settings.parryStarvation(); configDraft.parryGenericKill = settings.parryGenericKill();
        configDraft.perfect = settings.perfect(); configDraft.window = settings.window(); configDraft.recharge = settings.recharge();
        configDraft.angle = settings.angle(); configDraft.reductionPercent = settings.reductionPercent();
        configDraft.followUp = settings.followUp(); configDraft.parryReturnPercent = Math.clamp(settings.parryReturnPercent(), 0, 200);
        configDraft.perfectReturnPercent = Math.clamp(settings.perfectReturnPercent(), 0, 200); configDraft.parryWear = settings.parryWear();
        configDraft.perfectWear = settings.perfectWear(); configDraft.blockWear = settings.blockWear();
        configDraft.hud = GuardConfig.HUD.get();
        configDraft.shieldEffects = GuardConfig.SHIELD_EFFECTS.get();
        configDraft.memeFlash = GuardConfig.MEME_FLASH.get();
        configDraft.hitlagTicks = GuardConfig.HITLAG_TICKS.get();
        configDraft.impactFrames = GuardConfig.IMPACT_FRAMES.get();
        configDraft.impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
        configDraft.impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); configDraft.impactContrast = GuardConfig.IMPACT_CONTRAST.get();
        configDraft.impactEdges = GuardConfig.IMPACT_EDGES.get(); configDraft.impactGrain = GuardConfig.IMPACT_GRAIN.get();
        configDraft.hitParticles = settings.hitParticles();
        configDraft.masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); configDraft.perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
        configDraft.parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); configDraft.blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        configDraft.flashStrength=GuardConfig.FLASH_STRENGTH.get();
        configDraft.perfectOnlyFlash = GuardConfig.PERFECT_ONLY_FLASH.get();
        configDraft.shakeStrength=GuardConfig.SHAKE_STRENGTH.get();
        configDraft.fallParry = settings.fallParry(); configDraft.fallBreakBlocks = settings.fallBreakBlocks();
        configDraft.fallBlastStrength = settings.fallBlastStrength(); configDraft.fallLaunchPower = settings.fallLaunchPower();
        configDraft.parryExplosions = settings.parryExplosions(); configDraft.perfectExplosionsOnly = settings.perfectExplosionsOnly();
        configDraft.blockExplosions = settings.blockExplosions(); configDraft.parryProjectiles = settings.parryProjectiles();
        configDraft.blockProjectiles = settings.blockProjectiles();
        configDraft.knockbackStrength = settings.defenderKnockback() ? settings.knockbackStrength() : 0;
        configDraft.guardMovementPercent = settings.guardMovementPercent();
        configDraft.blockDeflectChance = settings.blockDeflectChance();
        configDraft.allowUsableItems = settings.allowUsableItems();
        configDraft.allowAnyItem = settings.allowAnyItem();

        configDraft.firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
        configDraft.thirdPersonAnimation = GuardConfig.THIRD_PERSON_ANIMATION.get();
        configDraft.includedItems = settings.includedItems(); configDraft.excludedItems = settings.excludedItems(); configDraft.shieldItems = settings.shieldItems();
        configDraft.consumablePriority = settings.consumablePriority();
        configDraft.parryHealing = settings.parryHealing(); configDraft.parryHealingHearts = settings.parryHealingHearts();
        setShieldSettings(GuardClient.currentShieldSettings());
        setMobSettings(GuardClient.currentMobSettings());
        var savedPoses = dev.zeli.mallardguard.GuardPoseLibrary.snapshot();
        if (savedPoses.isEmpty()) savedPoses = java.util.stream.IntStream.range(0,GuardPoseSettings.COUNT).mapToObj(dev.zeli.mallardguard.GuardPoseLibrary::preset).toList();
        poseDraft = new GuardPoseManagerScreen.Draft(savedPoses);poseDraft.enabled=configDraft.extraClient[65]!=0;
        selectRememberedGroup();
    }

    private void setShieldSettings(GuardPackets.ShieldSettings settings) {
        configDraft.retaliationEnabled=settings.retaliationEnabled();configDraft.perfectProjectilesOnly=settings.perfectProjectilesOnly();configDraft.perfectFallOnly=settings.perfectFallOnly();configDraft.perfectKnockbackMultiplier=settings.perfectKnockbackMultiplier();configDraft.incomingReaction=settings.incomingReaction();configDraft.parriedEnabled=settings.parriedEnabled();configDraft.parriedGuardBreakTicks=settings.parriedGuardBreakTicks();configDraft.parriedStunTicks=settings.parriedStunTicks();configDraft.shieldConeEnabled=settings.coneEnabled();
        configDraft.shieldPerfect = settings.perfect(); configDraft.shieldWindow = settings.window(); configDraft.shieldRechargeTicks = settings.rechargeTicks(); configDraft.shieldMaxBlocks = settings.maxBlocks(); configDraft.toolMaxBlocks = settings.toolMaxBlocks();
        configDraft.shieldBreakTicks = settings.breakTicks(); configDraft.shieldCone = settings.cone(); configDraft.shieldReach = settings.reach(); configDraft.shieldReturn = settings.retaliation();
        configDraft.cooldownPreventsGuard = settings.cooldownPreventsGuard(); configDraft.allowEmptyHand = settings.allowEmptyHand();
        configDraft.shieldStun = settings.stunTicks(); configDraft.shieldPerfectPush = settings.perfectPushback();
        configDraft.shieldRegularPush = settings.regularPushback(); configDraft.toolPush = settings.weaponPushback();
        configDraft.retaliationCap = settings.retaliationCap();
        configDraft.parryHandPriority = settings.parryHandPriority(); configDraft.shieldParryPriority = settings.shieldParryPriority(); configDraft.forceCrouchOffhand = settings.forceCrouchOffhand(); configDraft.shieldBlacklist = settings.shieldBlacklist(); configDraft.itemBlockCounts=settings.itemBlockCounts(); configDraft.stunnableBosses = settings.stunnableBosses();
    }

    private void setMobSettings(GuardPackets.MobSettings data) {
        configDraft.mobGuard=data.enabled();configDraft.mobDifficulty=data.difficulty();configDraft.mobWhitelist=data.whitelist();configDraft.mobGearChance=data.gearChance();
        configDraft.mobGearWhitelist=data.gearWhitelist();configDraft.mobGearBlacklist=data.gearBlacklist();
        configDraft.mobFlyingColor=data.flyingColor();configDraft.mobDebrisColor=data.debrisColor();
        configDraft.mobTracerStartColor=data.tracerStartColor();configDraft.mobTracerMiddleColor=data.tracerMiddleColor();configDraft.mobTracerEndColor=data.tracerEndColor();
    }

    public void updateMobSettings(GuardPackets.MobSettings data) {
        if (!dirtyMobs) { SavedState before=captureState();setMobSettings(data);acceptReceivedValues(before);rebuildWidgets(); }
    }

    public void updateShieldSettings(GuardPackets.ShieldSettings settings) {
        if (!dirtyShield) { SavedState before=captureState();setShieldSettings(settings); configDraft.consumablePriority = settings.consumablePriority();acceptReceivedValues(before);rebuildWidgets(); }
    }


    @Override protected void init() {
        controlBindings.clear();
        if(appliedState==null)appliedState=captureState();
        controlIndex = 0;
        settingsSearchBox = null;
        visibleSettingRows.clear(); rowValues.clear(); changedRows.clear(); originalRows.clear();rowRestores.clear();rowUndoButtons.clear();rowControls.clear();rowEnabled.clear();pageControls(true);
        themedButtons.clear(); redButtons.clear();colorPreviews.clear();
        buttonTips.clear();
        widgetTips.clear();
        settingLabels.clear();
        dropdownLabels.clear();
        sectionLabels.clear();
        GuardUiLayout.Panels panels=GuardUiLayout.panels(width);
        sidebarLeft=panels.sidebarLeft();sidebarWidth=panels.sidebarWidth();
        panelLeft=panels.contentLeft();panelWidth=panels.contentWidth();
        layoutTop = Math.min(14, Math.max(6, height / 16));
        visibleRows = Math.max(1, (height - rowTop() - 5) / ROW_STEP);
        activeClientCategory = switch (page) {
            case 7, 8 -> GuardClientPreset.PARTICLES;
            case 2,9,10,24,25->GuardClientPreset.SCREEN;
            case 15->GuardClientPreset.ANIMATIONS;
            case 17->GuardClientPreset.HUD;
            case 14 -> GuardClientPreset.PUNCHY;
            case 3 -> GuardClientPreset.AUDIO;
            case 4 -> 0;
            default -> 0;
        };
        boolean compact = height < 260;
        int modeHeight = compact ? 18 : 22;
        int modeTop = layoutTop + 34;
        int currentSection = experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        int sectionArrowWidth=GuardUiLayout.SECTION_ARROW_WIDTH, modeGap=GuardUiLayout.CONTROL_GAP;
        Button previous=addRenderableWidget(GuardUi.builder(Component.literal("<"),b->selectSection((currentSection+3)%4))
            .bounds(sidebarLeft,modeTop,sectionArrowWidth,modeHeight).build());
        Button mode=addRenderableWidget(GuardUi.builder(Component.literal(new String[]{"Client","Server","Enforce","Experimental"}[currentSection]+" ▾"),
            b->GuardUi.sections(this,currentSection,this::selectSection,b.getX(),b.getY(),b.getWidth(),b.getHeight()))
            .bounds(sidebarLeft+sectionArrowWidth+modeGap,modeTop,sidebarWidth-2*(sectionArrowWidth+modeGap),modeHeight).build());
        Button next=addRenderableWidget(GuardUi.builder(Component.literal(">"),b->selectSection((currentSection+1)%4))
            .bounds(sidebarLeft+sidebarWidth-sectionArrowWidth,modeTop,sectionArrowWidth,modeHeight).build());
        if(experimentalSection)redButtons.add(mode);
        tip(previous,"Previous config section.");tip(next,"Next config section.");
        tip(mode,"Choose a config section from the dropdown. The current section is dimmed; arrows cycle sections.");
        EditBox search = addRenderableWidget(GuardUi.editBox(font,panelLeft+10,layoutTop+2,panelWidth-20,"Search Mallard Guard settings"));
        settingsSearchBox = search;
        search.setMaxLength(80);
        search.setHint(Component.literal("Search settings across all sections..."));
        search.setValue(settingsSearch);
        search.setResponder(value -> { settingsSearch = value; settingsSearchChanged = true; });
        tip(search, "Search all settings by name or category. Choose a result to open its page and scroll to it. Clear to return to your current page.");
        GuardConfigPages.build(this);
        pageControls(true);

        {
            String[] tabs = enforceSection ? ENFORCE_TABS : clientSection ? CLIENT_TABS : experimentalSection ? EXPERIMENTAL_TABS : SERVER_TABS;
            int[] pages = enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            int shown = tabs.length;
            sidebarOffset = 0;
            for (int i = sidebarOffset; i < Math.min(tabs.length, sidebarOffset + shown); i++) {
                final int selected = pages[i];
                int buttonY = sidebarTop() + (i - sidebarOffset) * sidebarStep(tabs.length);
                String shortName = tabs[i].equals("Multiplayer") ? "Multiplayer"
                    : tabs[i].equals("Damage Types") ? "Damage" : tabs[i];
                Button tab = addRenderableWidget(GuardUi.builder(Component.literal(shortName), b -> {
                    settingsSearch = "";
                    rememberLocation();
                    page = selected;
                    firstRow = 0;
                    selectRememberedGroup();
                    rememberLocation();
                    rebuildWidgets();
                }).bounds(sidebarLeft + 8, buttonY, sidebarWidth - 16, sidebarStep(tabs.length)-3).build());
                tab.active = page != selected;
                tip(tab, "Open " + tabs[i] + " settings.");
            }

        }

        int iconSize=26;
        Button modPage=addRenderableWidget(new Button(sidebarLeft+1,layoutTop+4,iconSize,iconSize,Component.literal("Mallard Guard on CurseForge"),
            b->GuardUi.openLink(this,"Mallard Guard", "https://www.curseforge.com/minecraft/mc-mods/mallard-guard"),message->message.get()){
            @Override public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float delta){
                GuardUi.logo(graphics,getX(),getY(),getWidth());
                if(isHoveredOrFocused())graphics.renderOutline(getX()-1,getY()-1,getWidth()+2,getHeight()+2,GuardUi.WARM);
            }
        });
        logoButton=modPage;
        tip(modPage,"Open Mallard Guard's CurseForge page.");
        Button report=addRenderableWidget(GuardUi.iconButton("github","Issue Tracker",sidebarLeft+sidebarWidth-21,layoutTop+7,18,()->GuardUi.openLink(this,"Issue Tracker","https://github.com/zeli-024/Mallard-Guard/issues")));
        reportButton=report;tip(report,"Open the GitHub issue tracker.");
        Button help=addRenderableWidget(GuardUi.button("?",sidebarLeft+sidebarWidth-45,layoutTop+7,18,18,()->GuardUi.choices(this,"Config Help","EDITING\nClick a category, then a divider to open its controls.\nScroll within the settings list. Hover controls for details.\n\nVALUES\nScroll a slider for precise changes; Shift-scroll moves ten steps.\nYellow marks changed values. Undo restores the saved value.\n\nSAVING\nApply saves your edits. Reset restores defaults in the draft.\nServer settings require operator permission.\nLocked client settings are controlled by the server.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        tip(help,"Help with navigation, editing and saving.");
        int actionWidth=(sidebarWidth-22)/2, actionHeight=height<220?16:20, actionTop=height-(height<220?43:56), actionBottom=height-(height<220?23:32);
        Button reset=addRenderableWidget(GuardUi.button("Reset",sidebarLeft+8,actionTop,sidebarWidth-16,actionHeight,()->confirmReset(true)));
        reset.active=!(experimentalSection&&page==14&&!PunchyGuardCompat.installed())&&(clientSection||page==18||experimentalSection&&page!=12&&!GuardClient.clientCategoryLocked(categoryMask(page))||operator);tip(reset,"Reset this page or all editable settings to defaults.");redButtons.add(reset);
        Button apply=addRenderableWidget(GuardUi.button("Apply",sidebarLeft+8,actionBottom,actionWidth,actionHeight,this::applyPrompt));tip(apply,"Save settings, then choose Stay or Close.");
        Button close=addRenderableWidget(GuardUi.button("Close",sidebarLeft+14+actionWidth,actionBottom,actionWidth,actionHeight,this::onClose));tip(close,"Close settings. Unsaved changes require confirmation.");
        if(appliedState!=null){SavedState current=captureState();for(var entry:appliedState.fields().entrySet())restoreField(entry.getKey(),entry.getValue());
            for(var entry:rowValues.entrySet())originalRows.putIfAbsent(entry.getKey(),entry.getValue().get());
            for(var entry:current.fields().entrySet())restoreField(entry.getKey(),entry.getValue());}
        for(var entry:rowRestores.entrySet()){
            int position=entry.getKey();
            boolean header=dropdownLabels.stream().anyMatch(d->d.y()==row(position));
            for(var rendered:renderables)if(rendered instanceof AbstractWidget control
                &&control.getX()>=panelLeft&&control.getY()==row(position)){
                rowControls.put(position,new RowControl(control,control.getX(),control.getWidth(),header));break;
            }
            int undoX=contentRight()-GuardUiLayout.UNDO_SIZE-(header?GuardUiLayout.ARROW_SLOT+GuardUiLayout.CONTROL_GAP:0);
            Button undo=addRenderableWidget(GuardUi.iconButton("undo","Undo",undoX,row(position),GuardUiLayout.UNDO_SIZE,entry.getValue()));
            undo.visible=rowChanged(position);undo.active=undo.visible&&rowEnabled.getOrDefault(position,false);
            rowUndoButtons.put(position,undo);tip(undo,"Restore this setting to its last saved value.");
        }
        updateRowLayouts();
        totalRows=controlIndex;
        int clamped=Math.clamp(firstRow,0,maxScroll());
        if(clamped!=firstRow){firstRow=clamped;rebuildWidgets();}

    }

    void buildIconPosition(boolean enforced){
        int[] values=enforced?configDraft.enforcedDefaults:configDraft.extraClient;
        controls(enforced?values[0]!=0:configDraft.hud);
        int position=controlIndex++;
        trackValue(position,()->java.util.Arrays.stream(GuardIconPositionScreen.SLOTS).mapToObj(i->Integer.toString(values[i])).collect(java.util.stream.Collectors.joining(",")));
        rowUndo(position,enforced,()->{String[] previous=originalRows.get(position).split(",");for(int n=0;n<GuardIconPositionScreen.SLOTS.length;n++)values[GuardIconPositionScreen.SLOTS[n]]=Integer.parseInt(previous[n]);});
        if(!visible(position))return;
        String description="Drag the shield or choose a screen/hotbar anchor. Boss bars and shared HUD spacing are respected.";
        settingLabel(position,"Icon Position",description,editable(enforced));
        Button button=addRenderableWidget(GuardUi.button("Edit Position",settingX(),row(position),settingWidth(),20,()->openIconPosition(enforced)));
        button.active=editable(enforced);tip(button,scope(enforced,description));
    }
    private void openIconPosition(boolean enforced){
        int[] draft=(enforced?configDraft.enforcedDefaults:configDraft.extraClient).clone();
        Minecraft.getInstance().setScreen(new GuardIconPositionScreen(this,draft,()->enforced?operator:!GuardClient.clientCategoryLocked(GuardClientPreset.HUD),values->{
            int[] target=enforced?configDraft.enforcedDefaults:configDraft.extraClient;
            for(int i:GuardIconPositionScreen.SLOTS)target[i]=values[i];
            dirty=true;if(enforced)dirtyServer=dirtyPolicy=true;else dirtyClient=true;
        }));
    }

    void openHighlightPreview(boolean enforced){
        int[] draft=(enforced?configDraft.enforcedDefaults:configDraft.extraClient).clone();
        Minecraft.getInstance().setScreen(new GuardHighlightPreviewScreen(this,draft,editable(enforced),values->{
            int[] target=enforced?configDraft.enforcedDefaults:configDraft.extraClient;
            for(int i:new int[]{GuardScreenSettings.HIGHLIGHT_DIM,GuardScreenSettings.HIGHLIGHT_GLOW,GuardScreenSettings.HIGHLIGHT_FADE,GuardScreenSettings.HIGHLIGHT_DURATION,GuardScreenSettings.HIGHLIGHT_FADE_OUT})target[i]=values[i];
            dirty=true;if(enforced)dirtyServer=dirtyPolicy=true;else dirtyClient=true;
        }));
    }

    void openImpactPreview(boolean enforced) {
        int[] draft = enforced ? configDraft.enforcedDefaults.clone() : configDraft.extraClient.clone();
        if (!enforced) { draft[40] = configDraft.impactBrightness; draft[41] = configDraft.impactContrast; draft[42] = configDraft.impactEdges; draft[43] = configDraft.impactGrain; }
        Minecraft.getInstance().setScreen(new GuardImpactPreviewScreen(this, draft, enforced ? operator : editable(false), values -> {

            if (enforced) {
                configDraft.enforcedDefaults[35]=values[35];configDraft.enforcedDefaults[GuardScreenSettings.PLAYER_IMPACT_INVERT]=values[GuardScreenSettings.PLAYER_IMPACT_INVERT];
                for (int i = 40; i <= 43; i++) configDraft.enforcedDefaults[i] = values[i];
                for (int i = 50; i <= 57; i++) configDraft.enforcedDefaults[i] = values[i];
                dirty = dirtyServer = dirtyPolicy = true;
            } else {
                configDraft.impactPerfectOnly=values[35]!=0;configDraft.extraClient[GuardScreenSettings.PLAYER_IMPACT_INVERT]=values[GuardScreenSettings.PLAYER_IMPACT_INVERT];
                configDraft.impactBrightness = values[40]; configDraft.impactContrast = values[41]; configDraft.impactEdges = values[42]; configDraft.impactGrain = values[43];
                for (int i = 50; i <= 57; i++) configDraft.extraClient[i] = values[i];
                dirty = dirtyClient = true;
            }
            rebuildWidgets();

        }));
    }

    private int pageRows() { return Math.max(1,totalRows); }

    private List<GuardConfigSearch.Entry> searchResults() { return GuardConfigSearch.results(settingsSearch); }

    void buildSearchResults() {
        List<GuardConfigSearch.Entry> results = searchResults();
        if (results.isEmpty()) {
            int position = controlIndex++;
            if (visible(position)) settingLabel(position, "No matching settings", "Try a setting name or a category.", true);
        }
        for (GuardConfigSearch.Entry entry : results) {
            int position = controlIndex++;
            if (!visible(position)) continue;
            String scopeName = entry.section() == 0 ? "Client" : entry.section() == 1 ? "Server" : entry.section() == 3 ? "Experimental" : "Enforce";
            Button result = addRenderableWidget(GuardUi.builder(Component.literal(entry.name()), b -> jumpToSetting(entry))
                .bounds(panelLeft + 4, row(position), contentRight() - panelLeft - 8, 20).build());
            tip(result, scopeName + " > " + entry.category() + " > " + entry.name());
        }
    }

    private void jumpToSetting(GuardConfigSearch.Entry entry) {
        settingsSearch = "";
        settingsSearchChanged = false;
        experimentalSection = entry.section() == 3;
        enforceSection = entry.section() == 2;
        clientSection = entry.section() == 0;
        page = entry.page();
        if (!enforceSection) {
            int[] categories = enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = 0;
        String[] sections = enforceSection ? page==25?new String[]{"Target Highlight"}:page==8?new String[]{"Flying Sparks","Spark Debris","Spark Tracers","Center Impact"}:page==19?FEEDBACK_GROUPS:new String[] {"World Particles","Shield Icon","Audio Sliders","Hand Animation","Experimental Punchy"}
            : clientSection ? page == 7 ? new String[] {"Flying Sparks", "Spark Debris", "Spark Tracers", "Center Impact"}
                : page == 17 ? new String[]{"Shield Icon"} : page == 2 ? FEEDBACK_GROUPS : page == 15 ? new String[]{"Hand Animation"} : new String[0]
            : switch (page) {
                case 0 -> new String[] {"Parrying","Parried","Health","Knockback","Parry Types"};
                case 1 -> new String[] {"Guard Stance", "Projectiles", "Explosions"};
                case 4 -> new String[] {"Valid Parry Conditions", "Priority", "Item Durability Wear"};
                case 6 -> new String[] {"Common Damage Types"};
                case 18 -> new String[]{"Combat","Animations","Effects & Performance","Config & Assignments"};
                case 12 -> new String[] {"Parry Colors", "Spawn Equipment", "Mob Eligibility"};
                case 24 -> new String[]{"Target Highlight"};
                case 11 -> new String[] {"Shield Options", "Shield Perfect Parry", "Additional Shields and Bosses"};
                default -> new String[0];
            };
        String chosen = entry.group().isEmpty()?initialGroup():entry.group();
        for (GuardConfigSearch.Entry result : searchResultsForPage(entry)) {
            for (String group : sections) if (group.equals(result.name())) chosen = group;
            if (result.name().equals(entry.name())) break;
        }
        if(!entry.group().isEmpty())chosen=entry.group();
        if (chosen != null) rememberedGroups.put(groupKey(), chosen);
        selectRememberedGroup();
        rememberLocation();
        rebuildWidgets();
        if (enforceSection && !configDraft.enforceEnabled) return;
        Integer row = visibleSettingRows.get(entry.name());
        if (row != null) scrollTo(Math.max(0, row - 2));
    }

    private List<GuardConfigSearch.Entry> searchResultsForPage(GuardConfigSearch.Entry selected) {
        String old = settingsSearch;
        settingsSearch = "";
        try {
            return searchResults().stream().filter(result -> result.section() == selected.section()
                && result.page() == selected.page() && result.category().equals(selected.category())).toList();
        } finally { settingsSearch = old; }
    }

    private int maxScroll() { return Math.max(0, pageRows() - visibleRows); }
    private int rowTop() { return layoutTop + 33; }
    private int sidebarTop() { return layoutTop + (height < 220 ? 54 : height < 260 ? 58 : 70); }
    private int visibleSidebarTabs(int count) { return count; }
    private int sidebarStep(int count){return Math.min(23,Math.max(10,(height-(height<220?47:60)-sidebarTop())/Math.max(1,count)));}

    private int contentRight() { return panelLeft + panelWidth - 22; }
    int settingX() { return panelLeft + ((contentRight() - panelLeft) * 53 / 100); }
    int settingWidth() { return Math.max(12,contentRight() - settingX() - GuardUiLayout.CONTROL_GAP); }
    boolean visible(int index) { return index >= firstRow && index < firstRow + visibleRows; }
    int row(int index) { return rowTop() + (index - firstRow) * ROW_STEP; }

    public void updateDamageState(GuardPackets.DamageState state) {
        SavedState before=captureState();
        damageCatalog.clear();
        if (!state.catalog().isEmpty()) for (String id : state.catalog().split(","))
            if (GuardDamageRules.validId(id) && !damageCatalog.contains(id)) damageCatalog.add(id);
        sourceMods.clear();
        for (String line : state.modNames().split("\n")) {
            int separator = line.indexOf('\t');
            if (separator > 0) sourceMods.put(line.substring(0, separator), line.substring(separator + 1));
        }
        if (!dirtyDamageRules) {
            LinkedHashMap<String, Boolean> saved = GuardDamageRules.parseRules(state.rules());
            if (saved != null) { configDraft.damageRules.clear(); configDraft.damageRules.putAll(saved); }
        }
        recentHits.clear();
        if (!state.hits().isEmpty()) for (String id : state.hits().split(","))
            if (GuardDamageRules.validId(id) && !recentHits.contains(id) && recentHits.size() < 10)
                recentHits.add(id);
        acceptReceivedValues(before);
        if (!clientSection && page == 6) rebuildWidgets();
    }

    public void updateDamageHit(String id) {
        if (!GuardDamageRules.validId(id)) return;
        recentHits.remove(id);
        recentHits.add(0, id);
        while (recentHits.size() > 10) recentHits.remove(recentHits.size() - 1);
        if (!clientSection && page == 6) rebuildWidgets();
    }

    private String modName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        String namespace = key == null ? "unknown" : key.getNamespace();
        if (namespace.equals("minecraft")) return "Minecraft";
        String known = sourceMods.get(namespace);
        return known != null ? known : ModList.get().getModContainerById(namespace)
            .map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
    }

    private void section(String title) {
        int position = controlIndex++;
        if (visible(position)) sectionLabels.add(new SettingLabel(title, "", row(position), true));
    }

    private String sourceTip(String id, String description) {
        return description + " Mod: " + modName(id) + ". Damage type: " + id + ".";
    }

    void buildDamageTypes() {
        buildDamageSources();
        dropdown("Common Damage Types", () -> commonDamageOpen, "Show the built-in damage types and their parry rules.");
        if (commonDamageOpen) {
        for (String id : GuardDamageRules.BUILTINS) {
            String name = switch (id) {
                case "minecraft:drown" -> "Drowning";
                case "minecraft:in_fire" -> "Standing in fire";
                case "minecraft:on_fire" -> "Burning";
                case "minecraft:lava" -> "Lava";
                case "minecraft:hot_floor" -> "Hot floor";
                case "minecraft:campfire" -> "Campfire";
                case "minecraft:starve" -> "Starvation";
                case "minecraft:wither" -> "Wither";
                case "minecraft:magic" -> "Poison / Instant Damage";
                case "minecraft:indirect_magic" -> "Indirect magic";
                case "minecraft:fall" -> "Fall damage";
                case "minecraft:fly_into_wall" -> "Wall collision";
                default -> "Vanilla /kill";
            };
            toggle(name, () -> configDraft.damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id)), value -> {
                configDraft.damageRules.put(id, value); dirtyDamageRules = true; rebuildWidgets();
            }, true, sourceTip(id, "Parry this source when enabled. This does not change normal blocking."
                + (id.equals("minecraft:magic") ? " Vanilla poison and instant damage share this damage type." : "")
                + " Default: " + (GuardDamageRules.defaultAllowed(id) ? "On" : "Off") + "."));
        }
        }
    }

    private void buildDamageSources() {
        int position=controlIndex++;
        if(visible(position)){
            sectionLabels.add(new SettingLabel("Damage Sources","Review recent hits or search all runtime damage types.",row(position),operator));
            Button open=addRenderableWidget(GuardUi.builder(Component.literal("Damage Types"),b->{

                Minecraft.getInstance().setScreen(new GuardDamageSourcesScreen(this,recentHits,damageCatalog,configDraft.damageRules,operator,this::modName,rules->{
                    if(!configDraft.damageRules.equals(rules)){configDraft.damageRules.clear();configDraft.damageRules.putAll(rules);dirty=dirtyServer=dirtyDamageRules=true;}
                }));
            }).bounds(settingX(),row(position),settingWidth(),20).build());
            tip(open,"Open ten recent damage sources and the searchable runtime list. Toggles share these server parry rules.");
        }
    }

    private void trackValue(int position,java.util.function.Supplier<String> value){rowValues.put(position,value);}
    private boolean rowChanged(int position){String saved=originalRows.get(position);var current=rowValues.get(position);return saved!=null&&current!=null&&!saved.equals(current.get());}
    private void rowUndo(int position,boolean server,Runnable restore){
        rowEnabled.put(position,editable(server));
        rowRestores.put(position,()->{
            if(!rowEnabled.getOrDefault(position,false))return;
            restore.run();dirty=true;
            if(server){dirtyServer=true;if(enforceSection)dirtyPolicy=true;else if(page==12)dirtyMobs=true;else if(page==11)dirtyShield=true;else if(page==6)dirtyDamageRules=true;else dirtyServerRules=true;}else dirtyClient=true;
            rebuildWidgets();
        });
    }
    private void updateRowLayouts(){
        for(var entry:rowUndoButtons.entrySet()){
            boolean changed=rowChanged(entry.getKey());Button undo=entry.getValue();
            undo.visible=changed;undo.active=changed&&rowEnabled.getOrDefault(entry.getKey(),false);
            RowControl layout=rowControls.get(entry.getKey());if(layout==null)continue;
            int reserve=changed?GuardUiLayout.UNDO_SIZE+GuardUiLayout.CONTROL_GAP:0;
            layout.widget().setX(layout.x()-(layout.header()?reserve:0));
            layout.widget().setWidth(Math.max(12,layout.width()-(layout.header()?0:reserve)));
        }
        for(ColorPreview preview:colorPreviews){
            int reserve=rowChanged(preview.row())?GuardUiLayout.UNDO_SIZE+GuardUiLayout.CONTROL_GAP:0;
            preview.button().setX(preview.x()-reserve);
        }
    }
    private String originalText(String name,String fallback){
        if(appliedState!=null)for(int i=GuardParticleColors.OFFSET;i<GuardParticleColors.LENGTH;i++)if(GuardParticleColors.colorSlot(i)&&name.equals(GuardParticleColors.LABELS[i-GuardParticleColors.OFFSET]))for(var entry:appliedState.fields().entrySet())if(entry.getKey().equals(enforceSection?"enforcedDefaults":"extraClient"))return String.format("#%06X",((int[])entry.getValue())[i]);
        String mobColorField=switch(name){
            case "Mob Flying Sparks Color"->"mobFlyingColor";
            case "Mob Spark Debris Color"->"mobDebrisColor";
            case "Mob Tracer Start Color"->"mobTracerStartColor";
            case "Mob Tracer Middle Color"->"mobTracerMiddleColor";
            case "Mob Tracer End Color"->"mobTracerEndColor";
            default->"";
        };
        if(appliedState!=null&&!mobColorField.isEmpty())for(var entry:appliedState.fields().entrySet())if(entry.getKey().equals(mobColorField))return String.format("#%06X",((Number)entry.getValue()).intValue());
        String field=switch(name){case "Item Whitelist"->"includedItems";case "Item Blacklist"->"excludedItems";case "Shield Item Whitelist","Extra shields and tags"->"shieldItems";case "Shield Item Blacklist"->"shieldBlacklist";case "Item Block Counts"->"itemBlockCounts";case "Stunnable bosses"->"stunnableBosses";case "Mob Guard Whitelist"->"mobWhitelist";case "Mob Gear Whitelist"->"mobGearWhitelist";case "Mob Gear Blacklist"->"mobGearBlacklist";default->"";};if(appliedState!=null)for(var entry:appliedState.fields().entrySet())if(entry.getKey().equals(field))return String.valueOf(entry.getValue());return fallback;}
    private static boolean sameSetting(String field,Object current,Object saved){
        if(field.equals("damageRules")&&current instanceof Map<?,?> a&&saved instanceof Map<?,?> b){
            java.util.Set<Object> keys=new java.util.HashSet<>();keys.addAll(a.keySet());keys.addAll(b.keySet());
            for(Object key:keys){Object av=a.containsKey(key)?a.get(key):GuardDamageRules.defaultAllowed((String)key);Object bv=b.containsKey(key)?b.get(key):GuardDamageRules.defaultAllowed((String)key);if(!java.util.Objects.equals(av,bv))return false;}
            return true;
        }
        return java.util.Objects.deepEquals(current,saved);
    }
    private boolean hasChanges(){
        if(appliedState==null)return false;
        SavedState current=captureState();
        for(var entry:current.fields().entrySet())if(!sameSetting(entry.getKey(),entry.getValue(),appliedState.fields().get(entry.getKey())))return true;
        return current.poses()==null ? appliedState.poses()!=null : !current.poses().same(appliedState.poses());
    }
    void featureDivider(String name,BooleanSupplier expanded,BooleanSupplier enabled,Consumer<Boolean> change,boolean server,String description){
        controls(true);
        boolean on=enabled.getAsBoolean();int position=controlIndex;
        dropdownRow(name,expanded,description,pageControlsEnabled&&on);
        if(visible(position)){
            trackValue(position,()->enabled.getAsBoolean()?"On":"Off");
            if(editable(server)&&on)rowUndo(position,server,()->change.accept("On".equals(originalRows.get(position))));
            int w=Math.min(58,settingWidth());
            Button control=addRenderableWidget(GuardUi.button(on?"On":"Off",contentRight()-w-GuardUiLayout.ARROW_SLOT-GuardUiLayout.CONTROL_GAP,row(position),w,18,()->{
                boolean value=!enabled.getAsBoolean();change.accept(value);markChanged(server);
                if(java.util.Arrays.asList(GuardShieldReactions.NAMES).contains(name))reactionGroup=value?name:"";
                else rememberedGroups.put(groupKey(),value?name:"");
                selectRememberedGroup();rebuildWidgets();
            }));control.active=editable(server);tip(control,scope(server,description));
        }
        controls(on);
    }
    void buildDiagnostics(){
        int master=controlIndex;
        toggle("Diagnostic Logging",()->configDraft.debugFlags!=0,value->configDraft.debugFlags=value?GuardDiagnostics.DEFAULT_EVENT_GROUPS:0,false,"Write local diagnostic events to latest.log. Off stops logging and profiling. Default: Off.");
        rowUndo(master,false,()->configDraft.debugFlags=(Integer)appliedState.fields().get("debugFlags"));
        pageControls(configDraft.debugFlags!=0);
        for(String group:new String[]{"Combat","Animations","Effects & Performance","Config & Assignments"}){
            dropdown(group,()->diagnosticGroup.equals(group),"Local diagnostics. Select channels for a test, then Apply; turn them off afterward.");
            if(!diagnosticGroup.equals(group))continue;
            for(var channel:GuardDiagnostics.channels())if(channel.category().equals(group)){
                int flag=channel.flag();
                toggle(channel.name(),()->(configDraft.debugFlags&flag)!=0,value->configDraft.debugFlags=value?configDraft.debugFlags|flag:configDraft.debugFlags&~flag,false,channel.description()+"\n"+(channel.profiling()?"Off by default; enable explicitly for a test.":"Event logging only; no continuous frame logs.")+" Default: Off.");
            }
        }
    }
    void buildShieldReactions(boolean enforced){
        int[] target=enforced?configDraft.enforcedDefaults:configDraft.extraClient;
        boolean parentEnabled=pageControlsEnabled;
        pageControls(parentEnabled&&(enforced?target[0]!=0&&target[1]!=0:configDraft.hud&&configDraft.shieldEffects));
        for(int r=0;r<GuardShieldReactions.NAMES.length;r++){
            int base=GuardShieldReactions.OFFSET+r*GuardShieldReactions.STRIDE;
            String name=GuardShieldReactions.NAMES[r];
            featureDivider(name,()->reactionGroup.equals(name),()->target[base]!=0,value->{target[base]=value?1:0;if(enforced)dirtyServer=dirtyPolicy=true;},enforced,"Animate the "+name.toLowerCase(Locale.ROOT)+" reaction. Default: On.");
            if(!reactionGroup.equals(name)||target[base]==0)continue;
            for(int c=1;c<6;c++){
                if(!GuardShieldReactions.used(r*6+c))continue;final int reaction=r,component=c,index=base+c;
                String componentName=c==1?(r==4?"Lift":"Expansion"):c==3?"Highlight":GuardShieldReactions.CONTROLS[c];
                controls(target[base]!=0);
                if(c<5){int toggleIndex=GuardScreenSettings.REACTION_TOGGLES+r*4+c-1;controlBindings.put(name+" "+componentName,GuardControlSettings.presetBinding(toggleIndex));toggle(name+" "+componentName,()->target[toggleIndex]!=0,value->target[toggleIndex]=value?1:0,enforced,GuardScreenSettings.tip(toggleIndex));controls(target[base]!=0&&target[toggleIndex]!=0);}
                slider(name+" "+componentName+(c<5?" Strength":""),()->target[index],c==5?1:0,GuardShieldReactions.max(r*6+c),c==5?" ticks":"%",1,enforced,value->{target[index]=value;if(enforced)dirtyServer=dirtyPolicy=true;},c==5?"Reaction lifetime. 20 ticks = 1 second.":component==1&&reaction==4?"How far the ready icon lifts. 100% retains its normal movement.":"Scale this icon component. 100% retains its normal appearance.");
            }
        }
    }

    void dropdown(String name, BooleanSupplier expanded, String description) {
        controls(true);dropdownRow(name,expanded,description,pageControlsEnabled);
    }
    private void dropdownRow(String name,BooleanSupplier expanded,String description,boolean enabled){
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        dropdownLabels.add(new DropdownLabel(name, row(position), expanded, GuardUi.tooltipLines(description),enabled));
    }

    private String groupKey() {
        return (enforceSection ? "enforce" : experimentalSection ? "experimental" : clientSection ? "client" : "server") + ":" + page;
    }

    private String initialGroup() {
        if(enforceSection)return configDraft.enforceEnabled?switch(page){case 19->"Hitlag";case 25->"Target Highlight";case 20->"Shield Icon";case 21->"Audio Sliders";case 22->"Hand Animation";case 23->"Experimental Punchy";default->"Flying Sparks";}:null;
        return switch (page) {
            case 7 -> "Flying Sparks";
            case 2 -> clientSection ? "Hitlag" : null;
            case 17 -> "Shield Icon";
            case 18 -> "Combat";
            case 15 -> "Hand Animation";
            case 0 -> "Parrying";
            case 1 -> "Guard Stance";
            case 4 -> clientSection ? null : "Valid Parry Conditions";
            case 6 -> "Common Damage Types";
            case 12 -> "Parry Colors";
            case 24 -> "Target Highlight";
            case 11 -> "Shield Options";
            default -> null;
        };
    }

    void selectRememberedGroup() {
        String selected = rememberedGroups.getOrDefault(groupKey(), initialGroup());
        if((page==7||page==8) && selected!=null && !selected.isEmpty() && !java.util.Set.of("Spark Tracers","Flying Sparks","Spark Debris","Center Impact").contains(selected)){selected="Spark Tracers";rememberedGroups.put(groupKey(),selected);}
        if((clientSection&&page==2)||(enforceSection&&page==19))feedbackGroup=selected==null?"":java.util.Arrays.asList(FEEDBACK_GROUPS).contains(selected)?selected:"";
        if(page==24||page==25)feedbackGroup="Target Highlight".equals(selected)?"Target Highlight":"";
        whenParriedOpen="Parried".equals(selected)&&!clientSection&&page==0;
        centerImpactOpen = "Center Impact".equals(selected) && (page == 7||page==8);
        sparkTracersOpen = "Spark Tracers".equals(selected) && (page == 7||page==8);
        animatedOpen = "Flying Sparks".equals(selected) && (page == 7||page==8);
        dotStreaksOpen = "Spark Debris".equals(selected) && (page == 7||page==8);
        shieldOpen = "Shield Icon".equals(selected) && clientSection && page == 17;
        fallOpen="Parry Types".equals(selected)&&page==0;
        if(page==18)diagnosticGroup=selected==null?"":selected;
        animationOpen = "Hand Animation".equals(selected) && clientSection && page == 15;
        healingOpen = "Health".equals(selected) && !clientSection && page == 0;
        timingOpen = "Parrying".equals(selected) && !clientSection && page == 0;
        pushbackOpen = "Knockback".equals(selected) && page == 0;
        guardOpen = "Guard Stance".equals(selected) && page == 1;
        blockProjectileOpen = "Projectiles".equals(selected) && page == 1;
        blockExplosionOpen = "Explosions".equals(selected) && page == 1;
        eligibilityOpen = "Valid Parry Conditions".equals(selected) && !clientSection && page == 4;
        priorityOpen = "Priority".equals(selected) && !clientSection && page == 4;
        durabilityOpen = "Item Durability Wear".equals(selected) && page == 4;
        mobColorsOpen = "Parry Colors".equals(selected) && page == 12;
        mobEquipmentOpen = "Spawn Equipment".equals(selected) && page == 12;
        mobEligibilityOpen = "Mob Eligibility".equals(selected) && page == 12;
        shieldTimingOpen = "Shield Options".equals(selected) && page == 11;
        shieldPerfectOpen = "Shield Perfect Parry".equals(selected) && page == 11;
        shieldIdsOpen = "Additional Shields and Bosses".equals(selected) && page == 11;
        commonDamageOpen = "Common Damage Types".equals(selected) && page == 6;
        enforceHudOpen = ("Screen Effects".equals(selected)||"Shield Icon".equals(selected)||"Hand Animation".equals(selected)) && configDraft.enforceEnabled;
        enforceAudioOpen = "Audio Sliders".equals(selected) && configDraft.enforceEnabled;
        enforcePunchyOpen = "Experimental Punchy".equals(selected) && configDraft.enforceEnabled;
    }

    private void rememberLocation() {
        rememberedSection = experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        if (experimentalSection) { rememberedExperimentalPage = page; rememberedExperimentalRow = firstRow; }
        else if (enforceSection) {rememberedEnforceRow=firstRow;rememberedEnforcePage=page;}
        else if (clientSection) { rememberedClientPage = page; rememberedClientRow = firstRow; }
        else {rememberedServerPage = page; rememberedServerRow = firstRow;}
    }

    private int[] particleAppearanceOrder(int first,int end){
        if(first==GuardParticleColors.OFFSET&&end==GuardParticleColors.LENGTH)return java.util.stream.Stream.of(particleAppearanceOrder(GuardParticleColors.BASE,GuardParticleColors.FLYING_SIZE_VARIANCE+1),particleAppearanceOrder(GuardParticleColors.DEBRIS_COLOR,GuardParticleColors.DEBRIS_SIZE+1),particleAppearanceOrder(GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.PREVIOUS_LENGTH),particleAppearanceOrder(GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_DURATION+2)).flatMapToInt(java.util.Arrays::stream).toArray();
        if(first==GuardParticleColors.BASE&&end==GuardParticleColors.FLYING_SIZE_VARIANCE+1)return new int[]{GuardParticleColors.FLYING_ENABLED,GuardParticleColors.FLYING_SIZE,GuardParticleColors.FLYING_SIZE_VARIANCE,GuardParticleColors.FLYING_LIFETIME,GuardParticleColors.BASE};
        if(first==GuardParticleColors.DEBRIS_COLOR)return new int[]{GuardParticleColors.DEBRIS_ENABLED,GuardParticleColors.DEBRIS_SIZE,GuardParticleColors.DEBRIS_LIFETIME,GuardParticleColors.DEBRIS_COLOR};
        if(first==GuardParticleColors.TRACER_START_COLOR)return new int[]{GuardParticleColors.TRACER_ENABLED,GuardParticleColors.SHIELD_FORWARD_BIAS,GuardParticleColors.TRACER_SIZE,GuardParticleColors.TRACER_LIFETIME,GuardParticleColors.TRACER_TRAIL_TICKS,GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.TRACER_MIDDLE_COLOR,GuardParticleColors.TRACER_END_COLOR};
        if(first==GuardParticleColors.IMPACT_SIZE)return new int[]{GuardParticleColors.IMPACT_ENABLED,GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_SPIN,GuardParticleColors.IMPACT_OPACITY,GuardParticleColors.IMPACT_DURATION};
        return java.util.stream.IntStream.range(first,end).toArray();
    }
    void particleAppearance(boolean server,int first,int end) {
        int[] settings=server?configDraft.enforcedDefaults:configDraft.extraClient;
        for(int i:particleAppearanceOrder(first,end)) {
            if(GuardParticleColors.retired(i)||i>=GuardParticleColors.FLYING_ENABLED&&i<=GuardParticleColors.IMPACT_ENABLED)continue;
            final int index=i;String label=GuardParticleColors.LABELS[i-GuardParticleColors.OFFSET];
            controlBindings.put(label, GuardControlSettings.presetBinding(i));
            if(GuardParticleColors.toggleSlot(i))toggle(label,()->settings[index]!=0,v->settings[index]=v?1:0,server,GuardParticleColors.tip(i));
            else if(GuardParticleColors.colorSlot(i))colorField(label,()->settings[index],v->settings[index]=v,server,GuardParticleColors.tip(i));
            else slider(label,()->settings[index],GuardParticleColors.min(i),GuardParticleColors.max(i),GuardParticleColors.unit(i),1,server,v->settings[index]=v,GuardParticleColors.tip(i));
        }
    }

    void buildEnforcedDefaults() {
        if(page==19&&configDraft.enforceEnabled){dontEnforce(GuardClientPreset.SCREEN);buildFeedback(true);return;}
        if(page==25&&configDraft.enforceEnabled){dontEnforce(GuardClientPreset.SCREEN);buildHighlight(true);return;}
        if(page==8&&configDraft.enforceEnabled){dontEnforce(GuardClientPreset.PARTICLES);GuardConfigPages.buildWorldParticles(this,true);return;}
        if(page==20&&configDraft.enforceEnabled){
            dontEnforce(GuardClientPreset.HUD);
            featureDivider("Shield Icon",()->enforceHudOpen,()->configDraft.enforcedDefaults[0]!=0,value->configDraft.enforcedDefaults[0]=value?1:0,true,"Show the shield icon and its reactions. Off keeps the reaction settings.");
            if(enforceHudOpen){buildIconPosition(true);presetToggle("Shield reactions",1);buildShieldReactions(true);}
        }
        if(page==22)dropdown("Hand Animation",()->enforceHudOpen,"Set the server's starting hand animation values.");
        if(page==22&&configDraft.enforceEnabled&&enforceHudOpen){dontEnforce(GuardClientPreset.ANIMATIONS);presetToggle("Third-person guard pose",49);presetToggle("First-person guard pose",11);}
        if(page==23&&configDraft.enforceEnabled){
            dontEnforce(GuardClientPreset.PUNCHY);
            featureDivider("Experimental Punchy",()->enforcePunchyOpen,()->configDraft.enforcedDefaults[65]!=0,value->configDraft.enforcedDefaults[65]=value?1:0,true,"Use paired guarding and parried presets. Off keeps your presets and assignments.");
            if(enforcePunchyOpen){
                controlBindings.put("After Hitlag Hold",Control.PUNCHY_RELEASE_DELAY.binding(false));
                slider("After Hitlag Hold",()->configDraft.enforcedDefaults[GuardPoseSettings.RELEASE_DELAY_SLOT],0,1000," ms",1,true,v->configDraft.enforcedDefaults[GuardPoseSettings.RELEASE_DELAY_SLOT]=v,"Extra hold after hitlag. No extra delay without hitlag. Applies to every preset.");
                poseEditor(true);
            }
        }
        if(page==21)dropdown("Audio Sliders", () -> enforceAudioOpen, "Set the server's starting local playback volumes.");
        if (page==21 && configDraft.enforceEnabled && enforceAudioOpen) {
            dontEnforce(GuardClientPreset.AUDIO);
            presetSlider("Master volume", 12, "%");
            presetSlider("Perfect volume", 14, "%");
            presetSlider("Parry volume", 13, "%");
            presetSlider("Block volume", 15, "%");
        }
    }

    void poseEditor(boolean server) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent("Pose Presets", position);
        if (!visible(position)) return;
        String description = "Edit paired Guarding and Parried poses, motion and shared item assignments. Requires Punchy.\nDefault: Five enabled presets; Preset 5 uses empty hands.";
        settingLabel(position, "Pose Presets", scope(server, description), editable(server));
        Button editor = addRenderableWidget(GuardUi.builder(Component.literal("Edit Presets"), b -> {
            int[] source = server ? configDraft.enforcedDefaults : configDraft.extraClient;
            GuardPoseManagerScreen.Draft draft = poseDraft.copy();
            draft.enabled=source[65]!=0;
            if (server) { draft.poses.clear(); for (int i=0;i < GuardPoseSettings.COUNT;i++) { var pose=dev.zeli.mallardguard.GuardPoseLibrary.preset(i); pose.values=dev.zeli.mallardguard.GuardPoseSettings.pose(source,i);pose.parriedValues=dev.zeli.mallardguard.GuardPoseSettings.defaultParriedPose(pose.values);pose.enabled=pose.values[0]!=0;draft.poses.add(pose); } var enforced=dev.zeli.mallardguard.GuardPoseLibrary.parseAnimations(configDraft.enforcedPoseData);if(enforced!=null)draft.poses=new java.util.ArrayList<>(enforced); }
            Minecraft.getInstance().setScreen(new GuardPoseManagerScreen(this, draft, editable(server), result -> {

                for (int i=0;i < GuardPoseSettings.COUNT;i++) { var pose=result.poses.get(i); int[] values=pose.values.clone();values[0]=pose.enabled?1:0;System.arraycopy(values,0,source,dev.zeli.mallardguard.GuardPoseSettings.OFFSET+i*23,23); }
                source[65]=result.enabled?1:0;
                if(server)configDraft.enforcedPoseData=dev.zeli.mallardguard.GuardPoseLibrary.encodeAnimations(result.poses);
                if (!server) poseDraft = result.copy();
                dirty = true; if (server) dirtyServer = dirtyPolicy = true; else dirtyClient = true;

            }));
        }).bounds(settingX(), row(position), settingWidth(), 20).build());
        editor.active = editable(server) && PunchyGuardCompat.supportedVersion(); tip(editor, scope(server, description));
    }

    private int feedbackValue(boolean server,int index){
        if(server)return configDraft.enforcedDefaults[index];
        return switch(index){case 3->configDraft.hitlagTicks;case 4->configDraft.flashStrength;case 5->configDraft.shakeStrength;case 24->configDraft.memeFlash?1:0;case 27->configDraft.perfectOnlyFlash?1:0;case 34->configDraft.impactFrames;case 35->configDraft.impactPerfectOnly?1:0;default->configDraft.extraClient[index];};
    }
    private void feedbackValue(boolean server,int index,int value){
        if(server){configDraft.enforcedDefaults[index]=value;dirtyServer=dirtyPolicy=true;return;}
        switch(index){case 3->configDraft.hitlagTicks=value;case 4->configDraft.flashStrength=value;case 5->configDraft.shakeStrength=value;case 24->configDraft.memeFlash=value!=0;case 27->configDraft.perfectOnlyFlash=value!=0;case 34->configDraft.impactFrames=value;case 35->configDraft.impactPerfectOnly=value!=0;default->configDraft.extraClient[index]=value;}
    }
    private void feedbackToggle(boolean server,int index,String name,String tip){controlBindings.put(name,GuardControlSettings.presetBinding(index));toggle(name,()->feedbackValue(server,index)!=0,value->feedbackValue(server,index,value?1:0),server,tip);}
    private void feedbackSlider(boolean server,int index,String name,String suffix,String tip){var binding=GuardControlSettings.presetBinding(index);controlBindings.put(name,binding);slider(name,()->feedbackValue(server,index),binding.minimum(),binding.maximum(),suffix,1,server,value->feedbackValue(server,index,value),tip);}
    private boolean feedbackSection(boolean server,String name,int toggle,String tip){
        featureDivider(name,()->feedbackGroup.equals(name),()->feedbackValue(server,toggle)!=0,value->feedbackValue(server,toggle,value?1:0),server,tip);
        return feedbackGroup.equals(name);
    }
    private void feedbackColor(boolean server,int index){
        controlBindings.put(GuardScreenSettings.label(index),GuardControlSettings.presetBinding(index));
        colorField(GuardScreenSettings.label(index),()->feedbackValue(server,index),v->feedbackValue(server,index,v),server,GuardScreenSettings.tip(index));
    }
    private void feedbackPreview(boolean server,String label,Runnable action,String tip){
        int position=controlIndex++;if(!visible(position))return;
        settingLabel(position,label,tip,editable(server));var button=addRenderableWidget(GuardUi.button("Preview",settingX(),row(position),settingWidth(),20,action));button.active=editable(server);tip(button,tip);
    }
    void buildFeedback(boolean server){
        if(feedbackSection(server,"Hitlag",GuardScreenSettings.HITLAG_ENABLED,GuardScreenSettings.tip(GuardScreenSettings.HITLAG_ENABLED))){
            feedbackToggle(server,GuardScreenSettings.HITLAG_PERFECT_ONLY,"Perfect Only","Only perfect parries freeze the view. Hidden regular values are preserved.");
            feedbackSlider(server,3,"Hitlag Duration"," ticks","Local freeze duration. 20 ticks = one second.");
            if(feedbackValue(server,GuardScreenSettings.HITLAG_PERFECT_ONLY)==0)feedbackSlider(server,62,"Regular Duration"," ticks","Local regular-parry freeze. 0 disables it.");
        }
        if(feedbackSection(server,"Impact Frame",34,"Brief high-contrast, grayscale treatment of your view.\nUses the corresponding hitlag duration.\nFor the best effect, set perfect hitlag to 6 ticks.")){
            feedbackToggle(server,35,"Perfect Only","Only perfect parries use Impact Frame. Default: On.");
            feedbackPreview(server,"Preview",()->openImpactPreview(server),"Adjust the appearance against a paused scene.\nUses hitlag duration even when hitlag is disabled.");
        }
        if(feedbackSection(server,"White Flash",2,"Briefly washes your view in white when a parry succeeds.\nIntensity controls how visible the flash is.")){
            feedbackToggle(server,27,"Perfect Only","Only perfect parries show White Flash.");
            feedbackSlider(server,4,"Perfect Intensity","%","How visible the brief white flash is on a perfect parry.");
            if(feedbackValue(server,27)==0)feedbackSlider(server,GuardScreenSettings.FLASH_REGULAR_STRENGTH,"Regular Intensity","%",GuardScreenSettings.tip(GuardScreenSettings.FLASH_REGULAR_STRENGTH));
            feedbackToggle(server,24,"Meme Flash","Show a random joke image with White Flash. Default: Off.");
        }
        if(feedbackSection(server,"Camera Shake",GuardScreenSettings.CAMERA_ENABLED,GuardScreenSettings.tip(GuardScreenSettings.CAMERA_ENABLED))){
            feedbackToggle(server,GuardScreenSettings.SHAKE_PERFECT_ONLY,"Perfect Only","Only perfect parries shake the camera.");
            feedbackSlider(server,GuardScreenSettings.SHAKE_PERFECT_DURATION,"Perfect Duration"," ticks",GuardScreenSettings.tip(GuardScreenSettings.SHAKE_PERFECT_DURATION));
            feedbackSlider(server,5,"Perfect Strength","%","Perfect camera shake strength. 0 disables motion.");
            if(feedbackValue(server,GuardScreenSettings.SHAKE_PERFECT_ONLY)==0){feedbackSlider(server,GuardScreenSettings.SHAKE_REGULAR_DURATION,"Regular Duration"," ticks",GuardScreenSettings.tip(GuardScreenSettings.SHAKE_REGULAR_DURATION));feedbackSlider(server,GuardScreenSettings.SHAKE_REGULAR_STRENGTH,"Regular Strength","%",GuardScreenSettings.tip(GuardScreenSettings.SHAKE_REGULAR_STRENGTH));}
        }
        if(feedbackSection(server,"Chromatic Aberration",GuardScreenSettings.CHROMATIC_ENABLED,GuardScreenSettings.tip(GuardScreenSettings.CHROMATIC_ENABLED))){
            feedbackToggle(server,60,"Perfect Only","Only perfect parries trigger RGB separation.");
            feedbackSlider(server,59,"Perfect Duration"," ticks","Perfect-parry RGB separation lifetime.");
            if(feedbackValue(server,60)==0)feedbackSlider(server,GuardScreenSettings.CHROMATIC_REGULAR_DURATION,"Regular Duration"," ticks",GuardScreenSettings.tip(GuardScreenSettings.CHROMATIC_REGULAR_DURATION));
            feedbackToggle(server,61,"Include HUD","Apply the color separation to the world and HUD.\nDisabled keeps the HUD clear.");
        }
    }

    void buildHighlight(boolean server){
        if(feedbackSection(server,"Target Highlight",GuardScreenSettings.HIGHLIGHT_ENABLED,GuardScreenSettings.tip(GuardScreenSettings.HIGHLIGHT_ENABLED))){
            feedbackToggle(server,GuardScreenSettings.HIGHLIGHT_PERFECT_ONLY,"Perfect Only","Only perfect parries highlight the opponent.");
            feedbackToggle(server,GuardScreenSettings.HIGHLIGHT_ATTACKER_ENABLED,"Attacker Highlight",GuardScreenSettings.tip(GuardScreenSettings.HIGHLIGHT_ATTACKER_ENABLED));
            controls(feedbackValue(server,GuardScreenSettings.HIGHLIGHT_ENABLED)!=0&&feedbackValue(server,GuardScreenSettings.HIGHLIGHT_ATTACKER_ENABLED)!=0);
            feedbackColor(server,GuardScreenSettings.HIGHLIGHT_ATTACKER_COLOR);
            controls(feedbackValue(server,GuardScreenSettings.HIGHLIGHT_ENABLED)!=0);
            feedbackToggle(server,GuardScreenSettings.HIGHLIGHT_TARGET_ENABLED,"Target Highlight",GuardScreenSettings.tip(GuardScreenSettings.HIGHLIGHT_TARGET_ENABLED));
            controls(feedbackValue(server,GuardScreenSettings.HIGHLIGHT_ENABLED)!=0&&feedbackValue(server,GuardScreenSettings.HIGHLIGHT_TARGET_ENABLED)!=0);
            feedbackColor(server,GuardScreenSettings.HIGHLIGHT_TARGET_COLOR);
            controls(feedbackValue(server,GuardScreenSettings.HIGHLIGHT_ENABLED)!=0);
            choice("Timing",new String[]{"Before","After"},()->feedbackValue(server,GuardScreenSettings.HIGHLIGHT_AFTER),v->{feedbackValue(server,GuardScreenSettings.HIGHLIGHT_AFTER,v);rebuildWidgets();},GuardScreenSettings.tip(GuardScreenSettings.HIGHLIGHT_AFTER),server);
            feedbackSlider(server,GuardScreenSettings.HIGHLIGHT_DURATION,GuardScreenSettings.label(GuardScreenSettings.HIGHLIGHT_DURATION),GuardScreenSettings.unit(GuardScreenSettings.HIGHLIGHT_DURATION),GuardScreenSettings.tip(GuardScreenSettings.HIGHLIGHT_DURATION));
            for(int index:new int[]{GuardScreenSettings.HIGHLIGHT_DIM,GuardScreenSettings.HIGHLIGHT_FADE,GuardScreenSettings.HIGHLIGHT_FADE_OUT,GuardScreenSettings.HIGHLIGHT_GLOW})feedbackSlider(server,index,GuardScreenSettings.label(index),GuardScreenSettings.unit(index),GuardScreenSettings.tip(index));
            feedbackPreview(server,"Preview",()->openHighlightPreview(server),"Still preview holds full strength. Play Once tests both fades without combat.");
        }
    }

    private void dontEnforce(int category) {
        toggle("Don't Enforce", () -> (configDraft.dontEnforceMask & category) != 0, value -> {
            configDraft.dontEnforceMask = value ? configDraft.dontEnforceMask | category : configDraft.dontEnforceMask & ~category;
        }, true, "On allows client edits after receiving server defaults. Off locks this category.");
    }

    private void presetToggle(String name, int index) {
        controlBindings.put(name, GuardControlSettings.presetBinding(index));
        toggle(name, () -> configDraft.enforcedDefaults[index] != 0, value -> configDraft.enforcedDefaults[index] = value ? 1 : 0, true,
            "Starting client value. Locked unless Don't Enforce is enabled.");
    }

    private void presetSlider(String name, int index, String suffix) {
        var binding = GuardControlSettings.presetBinding(index);
        controlBindings.put(name, binding);
        slider(name, () -> configDraft.enforcedDefaults[index], binding.minimum(), binding.maximum(), suffix, binding.scale(), true, value -> configDraft.enforcedDefaults[index] = value,
            "Starting client value. Locked unless Don't Enforce is enabled.");
    }

    private boolean editable(boolean server) {
        return featureControlsEnabled&&permitted(server);
    }
    private boolean permitted(boolean server){
        return server?operator:!GuardClient.clientCategoryLocked(activeClientCategory);
    }

    private String scope(boolean server, String description) {
        return description + (server ? operator ? "" : " OP required." : permitted(false) ? "" : " Locked by server.")
            +(!featureControlsEnabled?"\nEnable the parent feature to edit.":"");
    }

    void settingLabel(int index, String name, String description, boolean enabled) {
        settingLabels.add(new SettingLabel(name, GuardUi.tooltipLines(description), row(index), enabled));
    }

    final Map<String, GuardControlSettings.Binding> controlBindings = new HashMap<>();

    private String declaredDefault(String name) {
        var binding = controlBindings.get(name);
        Object value = binding == null ? null : binding.defaultValue();
        if(value==null)return null;
        if(name.endsWith("Color")&&value instanceof Number color)return String.format("#%06X",color.intValue());
        int appearance=GuardParticleColors.indexOf(binding.setting());
        if(appearance>=0){
            int number=((Number)value).intValue();
            if(GuardParticleColors.toggleSlot(appearance))return number==0?"Off":"On";
            if(GuardParticleColors.colorSlot(appearance))return String.format("#%06X",number);
            return number+GuardParticleColors.unit(appearance);
        }
        if(value instanceof Boolean b)return b ? "On" : "Off";
        String suffix=switch(name) {
            case "After Hitlag Hold" -> " ms";
            case "Perfect window" -> " ticks";
            case "Parry window" -> " ticks";
            case "Recharge" -> " ticks";
            case "Facing angle" -> "°";
            case "Follow-up parries" -> " ticks";
            case "Parry healing amount" -> " hearts";
            case "Shield perfect window" -> " ticks";
            case "Shield parry window" -> " ticks";
            case "Shield parry recharge" -> " ticks";
            case "Shield blocks before break" -> "";
            case "Weapon blocks before break" -> "";
            case "Guard break recharge" -> " ticks";
            case "Cone reach" -> " blocks";
            case "Shield retaliation" -> "%";
            case "Retaliation damage cap" -> " damage";
            case "Stun duration" -> " ticks";
            case "Base Intensity", "Perfect Intensity", "Damage Intensity", "Debris Base Intensity", "Debris Perfect Intensity", "Debris Damage Intensity" -> "%";
            case "Screen flash" -> "%";
            case "Camera shake" -> "%";
            case "Mob gear chance" -> "%";
            case "Mob guard difficulty" -> "%";
            case "Any held item" -> "";
            case "Empty Hand" -> "";
            case "First-person guard pose" -> "";
            case "Master volume" -> "%";
            case "Perfect volume" -> "%";
            case "Parry volume" -> "%";
            case "Block volume" -> "%";
            default -> "";
        };
        if(binding!=null && binding.scale()>1 && value instanceof Number number)return String.format(Locale.ROOT,binding.scale()==100?"%.2f":"%.1f",number.doubleValue()/binding.scale())+suffix;
        return value.toString()+suffix;
    }

    private String defaultText(String name, boolean server) {
        String declared = declaredDefault(name);
        if (declared != null) return declared;
        if (name.equals("Don't Enforce") || name.equals("Enforce client settings")) return "Off";
        if (name.equals("Fall parry")) return "On";
        return "Off";
    }

    void sliderBound(Control control, String name, java.util.function.IntSupplier current, String suffix,
                             int divisor, boolean server, java.util.function.IntConsumer change, String description) {
        var binding = control.binding(server && !enforceSection);
        controlBindings.put(name, binding);
        int minimum = control == Control.SHIELD_BREAK_TICKS ? Math.max(configDraft.recharge, binding.minimum()) : binding.minimum();
        slider(name, current, minimum, binding.maximum(), suffix, divisor, server, change, description);
    }

    void parriedReaction() {
        choice("Reaction", new String[]{"Stun", "Guard Break", "Both"},
            () -> Math.clamp(configDraft.incomingReaction - 1, 0, 2),
            value -> {
                configDraft.incomingReaction = value + 1;
                dirtyShield = true;
                rebuildWidgets();
            },
            "Stun stops movement, attacks and guard. Guard Break delays guarding only.\nBoth applies them together with independent durations. All modes interrupt the parried attack.");
    }

    void choice(String name, String[] options, java.util.function.IntSupplier current, java.util.function.IntConsumer change, String description) {
        choice(name,options,current,change,description,true);
    }
    void choice(String name, String[] options, java.util.function.IntSupplier current, java.util.function.IntConsumer change, String description,boolean server) {
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        trackValue(position,()->options[current.getAsInt()]);
        if(editable(server))rowUndo(position,server,()->{String old=originalRows.get(position);for(int i=0;i<options.length;i++)if(options[i].equals(old)){change.accept(i);break;}});
        settingLabel(position,name,scope(server,description),editable(server));
        Button button=addRenderableWidget(GuardUi.builder(Component.literal(options[current.getAsInt()]),b->{
            dirty=true;if(server){dirtyServer=true;if(enforceSection)dirtyPolicy=true;else dirtyServerRules=true;}else dirtyClient=true;
            change.accept((current.getAsInt()+1)%options.length);b.setMessage(Component.literal(options[current.getAsInt()]));
        }).bounds(settingX(),row(position),settingWidth(),20).build());button.active=editable(server);tip(button,scope(server,description));
    }

    void toggle(Control control, String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        controlBindings.put(name, control.binding(server && !enforceSection));
        toggle(name, current, change, server, description);
    }

    void toggle(String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        trackValue(position,()->GuardUi.enabledLabel(current.getAsBoolean()));
        var binding = controlBindings.get(name);
        String defaultValue=defaultText(name,server);
        if(binding!=null&&binding.defaultValue() instanceof Number number&&binding.minimum()==0&&binding.maximum()==1)defaultValue=GuardUi.enabledLabel(number.intValue()!=0);
        String tooltip = binding == null
            ? scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultValue + "."))
            : scope(server, description.replaceAll("\\s*Default:[^.]*(?:\\.(?!\\d)|$)", "").trim() + " Default: " + defaultValue + ".");
        settingLabel(position, name, tooltip, editable(server));
        if(editable(server))rowUndo(position,server,()->change.accept("Enabled".equals(originalRows.get(position))));
        boolean damageToggle = server && !enforceSection && page == 6;
        Button button = addRenderableWidget(GuardUi.builder(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()), b -> {
            dirty = true;
            if (server) {
                dirtyServer = true;
                if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else dirtyServerRules = true;
            } else dirtyClient = true;
            change.accept(!current.getAsBoolean());
            b.setMessage(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()));
            rebuildWidgets();
        }).bounds(settingX(), row(position), settingWidth(), 20).build());
        boolean automaticPose = !server && binding != null && binding.setting() == GuardConfig.FIRST_PERSON_ANIMATION
            && (!PunchyGuardCompat.installed() || configDraft.extraClient[65] != 0);
        button.active = editable(server) && !automaticPose;
        if (automaticPose) tooltip += " Selected automatically for the available animation renderer.";
        tip(button, tooltip);
    }

    boolean simplePoseDraftEnabled() {
        if (!PunchyGuardCompat.installed()) return true;
        if (configDraft.extraClient[65] != 0) return !PunchyGuardCompat.supportedVersion();
        return configDraft.firstPersonAnimation;
    }

    void slider(String name, java.util.function.IntSupplier current, int min, int max, String suffix,
                        int divisor, boolean server, java.util.function.IntConsumer change, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        trackValue(position,()->Integer.toString(current.getAsInt()));
        var binding = controlBindings.get(name);
        String defaultValue = defaultText(name, server);
        if (binding != null && binding.defaultValue() instanceof Number number) {
            defaultValue = (divisor == 1 ? Integer.toString(number.intValue()) : String.format(java.util.Locale.ROOT, divisor==1000?"%.3f":"%.1f", number.doubleValue() / divisor)) + suffix;
        }
        String tooltip = scope(server, description.replaceAll("\\s*Default:[^.]*(?:\\.(?!\\d)|$)", "").trim() + " Default: " + defaultValue + ".");
        settingLabel(position, name, tooltip, editable(server));
        if(editable(server))rowUndo(position,server,()->change.accept(Integer.parseInt(originalRows.get(position))));
        GuardSlider control = addRenderableWidget(new GuardSlider(settingX(), row(position), settingWidth(), 20,
            current.getAsInt(), min, max, GuardControlSettings.dragStep(binding, min, max, divisor, suffix),
            value -> (divisor == 1 ? Integer.toString(value) : String.format(Locale.ROOT,
                divisor == 10 ? "%.1f" : divisor == 1000 ? "%.3f" : "%.2f", value / (double) divisor)) + suffix,
            value -> {
                dirty = true;
                if (server) {
                    dirtyServer = true;
                    if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else dirtyServerRules = true;
                } else dirtyClient = true;
                change.accept(value);
            }, () -> false));
        control.active = editable(server);
        tip(control, tooltip);
    }

    void itemRuleAssignment(String name,String current,Consumer<String> update,String description){
        // One editor owns both rule lists; keep the persisted server fields compatible.
        if(name.endsWith("Blacklist"))return;
        boolean shield=name.startsWith("Shield");String title=shield?"Shield Item Eligibility":"Item Eligibility";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(title,position);if(!visible(position))return;
        String help="Left-click items to allow; right-click to block.\nUse ! exceptions for overlapping mod/tag rules.";
        settingLabel(position,title,scope(true,help),editable(true));
        java.util.function.Supplier<String> allowed=()->shield?configDraft.shieldItems:configDraft.includedItems,blocked=()->shield?configDraft.shieldBlacklist:configDraft.excludedItems;
        String oldAllowed=originalText(name,current),oldBlocked=originalText(shield?"Shield Item Blacklist":"Item Blacklist",blocked.get());
        rowValues.put(position,()->"Allowed: "+allowed.get()+" | Blocked: "+blocked.get());originalRows.put(position,"Allowed: "+oldAllowed+" | Blocked: "+oldBlocked);
        if(editable(true))rowUndo(position,true,()->{if(shield){configDraft.shieldItems=oldAllowed;configDraft.shieldBlacklist=oldBlocked;dirtyShield=dirtyServerRules=true;}else{configDraft.includedItems=oldAllowed;configDraft.excludedItems=oldBlocked;dirtyServerRules=true;}});
        Button assign=addRenderableWidget(GuardUi.button("Assign Items",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,title,allowed.get(),blocked.get(),(whitelist,blacklist)->{
            if(whitelist.equals(allowed.get())&&blacklist.equals(blocked.get()))return;
            if(shield){configDraft.shieldItems=whitelist;configDraft.shieldBlacklist=blacklist;dirtyShield=dirtyServerRules=true;}else{configDraft.includedItems=whitelist;configDraft.excludedItems=blacklist;dirtyServerRules=true;}
            configDraft.invalidItemRules=false;dirty=dirtyServer=true;
        }))));assign.active=editable(true);tip(assign,help);
    }

    void mobGearAssignments(){
        String name="Mob Gear Assignments",help="Left: allow; right: block; either click removes an assignment.\nDefault: vanilla armor/tools, without axes, netherite, elytra, turtle helmets or maces. Generated gear only.";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        settingLabel(position,name,scope(true,help),editable(true));
        String oldAllowed=originalText("Mob Gear Whitelist",configDraft.mobGearWhitelist),oldBlocked=originalText("Mob Gear Blacklist",configDraft.mobGearBlacklist);
        rowValues.put(position,()->configDraft.mobGearWhitelist+" | "+configDraft.mobGearBlacklist);originalRows.put(position,oldAllowed+" | "+oldBlocked);
        if(editable(true))rowUndo(position,true,()->{configDraft.mobGearWhitelist=oldAllowed;configDraft.mobGearBlacklist=oldBlocked;});
        Button button=addRenderableWidget(GuardUi.button("Assign Gear",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(
            new GuardPoseItemScreen(this,"Mob Gear",configDraft.mobGearWhitelist,configDraft.mobGearBlacklist,(allowed,blocked)->{
                if(!allowed.equals(configDraft.mobGearWhitelist)||!blocked.equals(configDraft.mobGearBlacklist)){configDraft.mobGearWhitelist=allowed;configDraft.mobGearBlacklist=blocked;dirty=dirtyServer=dirtyMobs=true;}
            }).restrictItems(item->dev.zeli.mallardguard.GuardMobState.equipmentSlot(item)!=null)
                .assignmentDefaults(GuardConfig.MOB_GEAR_WHITELIST.getDefault(),GuardConfig.MOB_GEAR_BLACKLIST.getDefault()))));
        button.active=editable(true);tip(button,help);
    }

    void blockCountAssignments(){
        String name="Item Assignments",description="Assign allowed/blocked items and individual block limits.\nLeft: allow; right: block; either click again removes. 0 blocks is unlimited.";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        settingLabel(position,name,scope(true,description),editable(true));
        String oldAllowed=originalText("Item Whitelist",configDraft.includedItems),oldBlocked=originalText("Item Blacklist",configDraft.excludedItems),oldCounts=originalText("Item Block Counts",configDraft.itemBlockCounts);
        rowValues.put(position,()->configDraft.includedItems+" | "+configDraft.excludedItems+" | "+configDraft.itemBlockCounts);originalRows.put(position,oldAllowed+" | "+oldBlocked+" | "+oldCounts);
        if(editable(true))rowUndo(position,true,()->{configDraft.includedItems=oldAllowed;configDraft.excludedItems=oldBlocked;configDraft.itemBlockCounts=oldCounts;dirtyServerRules=dirtyShield=true;});
        Button button=addRenderableWidget(GuardUi.button("Assign Items",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Item Assignments",configDraft.includedItems,configDraft.excludedItems,configDraft.itemBlockCounts,result->{
            if(!result.whitelist().equals(configDraft.includedItems)||!result.blacklist().equals(configDraft.excludedItems)){configDraft.includedItems=result.whitelist();configDraft.excludedItems=result.blacklist();dirtyServerRules=true;}
            if(!result.blockCounts().equals(configDraft.itemBlockCounts)){configDraft.itemBlockCounts=result.blockCounts();dirtyShield=true;}
            configDraft.invalidItemRules=false;dirty=dirtyServer=true;
        }))));
        button.active=editable(true);tip(button,description);
    }

    private static Integer parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(hex, 16);
    }
    private record ColorPreview(Button button,java.util.function.IntSupplier color,int row,int x) {}
    private final List<ColorPreview> colorPreviews=new ArrayList<>();
    void colorField(Control control, String name, java.util.function.IntSupplier current,
                            java.util.function.IntConsumer change, boolean server, String description) {
        controlBindings.put(name, control.binding(server && !enforceSection));
        colorField(name, current, change, server, description);
    }

    void colorField(String name,java.util.function.IntSupplier current,java.util.function.IntConsumer change,boolean server,String description){
        int position=controlIndex;
        editBox(name,String.format("#%06X",current.getAsInt()),text->{Integer rgb=parseColor(text);if(rgb!=null)change.accept(rgb);},description,server);
        if(!visible(position))return;
        EditBox box=null;for(var child:children())if(child instanceof EditBox e&&e.getY()==row(position)&&e.getX()==settingX()){box=e;break;}
        if(box==null)return;
        box.setMaxLength(7);box.setWidth(Math.max(20,settingWidth()-24));
        Button preview=addRenderableWidget(GuardUi.button("",settingX()+settingWidth()-20,row(position),20,20,()->Minecraft.getInstance().setScreen(new GuardColorPickerScreen(this,name,current.getAsInt(),rgb->{change.accept(rgb);markChanged(server);}))));
        preview.active=editable(server);tip(preview,"Choose a color using the hue wheel and triangle.");colorPreviews.add(new ColorPreview(preview,current,position,preview.getX()));
    }
    private void markChanged(boolean server) {
        dirty = true;
        if (!server) { dirtyClient = true; return; }
        dirtyServer = true;
        if (enforceSection) dirtyPolicy = true;
        else if (page == 12) dirtyMobs = true;
        else if (page == 11) dirtyShield = true;
        else if (page == 6) dirtyDamageRules = true;
        else dirtyServerRules = true;
    }
    void editBox(Control control, String name, String initial, Consumer<String> update, String description) {
        controlBindings.put(name, control.binding(true));
        editBox(name, initial, update, description, true);
    }

    void editBox(String name, String initial, Consumer<String> update, String description) {
        editBox(name,initial,update,description,true);
    }
    void editBox(String name, String initial, Consumer<String> update, String description,boolean server) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        String tooltip = scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, server) + "."));
        settingLabel(position, name, tooltip, editable(server));
        EditBox box = addRenderableWidget(GuardUi.editBox(font,settingX(),row(position),settingWidth(),name));
        rowValues.put(position,()->box.getValue());
        originalRows.put(position,originalText(name,initial));
        if(editable(server))rowUndo(position,server,()->update.accept(originalRows.get(position)));
        box.setMaxLength(1024);
        box.setValue(initial);
        box.setResponder(value -> { update.accept(value);markChanged(server); });
        box.setEditable(editable(server));
        box.active = editable(server);
        tip(box, tooltip);
    }

    private static Component damageStatus(boolean enabled) {
        return Component.literal("Parry: " + (enabled ? "Enabled" : "Disabled"))
            .withStyle(style -> style.withColor(enabled ? 0xB6E8B6 : 0xA7A7A7));
    }

    void tip(AbstractWidget widget, String description) {
        description=GuardUi.tooltipLines(description);
        widget.setTooltip(null);
        widgetTips.put(widget, description);
        if (widget instanceof Button button) {
            themedButtons.add(button);
            buttonTips.put(button, description);
        }
    }

    private static Component label(boolean value) {
        return Component.literal(GuardUi.enabledLabel(value));
    }

    public void refreshPolicy() {
        SavedState before=captureState();
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PARTICLES)) {
            for(int i=16;i<=21;i++)configDraft.extraClient[i]=dev.zeli.mallardguard.GuardClientSettings.local(i);
        }
        int[] effectiveClient = GuardClientPreset.readLocal();
        for (int i = 50; i < configDraft.extraClient.length; i++) if (GuardClient.clientCategoryLocked(GuardClientPreset.categoryOf(i))) configDraft.extraClient[i] = effectiveClient[i];
        if(GuardClient.clientCategoryLocked(GuardClientPreset.HUD)){configDraft.hud=GuardConfig.HUD.get();configDraft.shieldEffects=GuardConfig.SHIELD_EFFECTS.get();}
        if(GuardClient.clientCategoryLocked(GuardClientPreset.ANIMATIONS)){configDraft.firstPersonAnimation=GuardConfig.FIRST_PERSON_ANIMATION.get();configDraft.thirdPersonAnimation=GuardConfig.THIRD_PERSON_ANIMATION.get();}
        if (GuardClient.clientCategoryLocked(GuardClientPreset.SCREEN)) {
            configDraft.extraClient[2]=effectiveClient[2];
            configDraft.memeFlash = GuardConfig.MEME_FLASH.get();
            configDraft.flashStrength=GuardConfig.FLASH_STRENGTH.get();
            configDraft.perfectOnlyFlash = GuardConfig.PERFECT_ONLY_FLASH.get();
            configDraft.shakeStrength=GuardConfig.SHAKE_STRENGTH.get();
            configDraft.hitlagTicks = GuardConfig.HITLAG_TICKS.get();
            configDraft.impactFrames = GuardConfig.IMPACT_FRAMES.get();
            configDraft.impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
            configDraft.impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); configDraft.impactContrast = GuardConfig.IMPACT_CONTRAST.get();
            configDraft.impactEdges = GuardConfig.IMPACT_EDGES.get(); configDraft.impactGrain = GuardConfig.IMPACT_GRAIN.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.AUDIO)) {
            configDraft.masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); configDraft.perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
            configDraft.parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); configDraft.blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) {
            for(int i=0;i<configDraft.extraClient.length;i++)if(GuardClientPreset.categoryOf(i)==GuardClientPreset.PUNCHY)configDraft.extraClient[i]=effectiveClient[i];
        }
        acceptReceivedValues(before);
        rebuildWidgets();
    }

    /** Reset values in memory; Apply is still required to commit. */
    private void reset(boolean wholeSection) {
        if(page==18){configDraft.debugFlags=0;dirty=dirtyClient=true;rebuildWidgets();return;}
        if ((enforceSection || !clientSection && !experimentalSection) && !operator) return;
        if (enforceSection) {
            configDraft.enforceEnabled = false;
            configDraft.dontEnforceMask = 0;
            configDraft.enforcedPoseData="";
            System.arraycopy(GuardClientPreset.DEFAULTS, 0, configDraft.enforcedDefaults, 0, configDraft.enforcedDefaults.length);
            dirtyServer = dirtyPolicy = dirty = true;
        } else if (clientSection) {
            if (wholeSection) {
                for (int category : CLIENT_PAGES) if (!GuardClient.clientCategoryLocked(categoryMask(category))) resetClientPage(category);
            } else if (!GuardClient.clientCategoryLocked(categoryMask(page))) resetClientPage(page);
        } else if (experimentalSection) {
            if ((wholeSection || page == 14) && !GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) resetClientPage(14);
            if((wholeSection||page==24)&&!GuardClient.clientCategoryLocked(GuardClientPreset.SCREEN))resetClientPage(24);
            if (operator) {
                GuardPackets.Settings defaults = GuardConfig.defaultSnapshot(true);
                if (wholeSection) for (int category : EXPERIMENTAL_PAGES) resetServerPage(category, defaults);
                else resetServerPage(page, defaults);
            }
        } else {
            GuardPackets.Settings defaults = GuardConfig.defaultSnapshot(operator);
            if (wholeSection) for (int category : SERVER_PAGES) resetServerPage(category, defaults);
            else resetServerPage(page, defaults);
        }
        rebuildWidgets();
    }

    private static boolean highlightSlot(int i){
        return i==GuardScreenSettings.HIGHLIGHT_ENABLED||i==GuardScreenSettings.HIGHLIGHT_DIM||i==GuardScreenSettings.HIGHLIGHT_GLOW||i==GuardScreenSettings.HIGHLIGHT_FADE||i==GuardScreenSettings.HIGHLIGHT_DURATION
            ||i>=GuardScreenSettings.HIGHLIGHT_FADE_OUT&&i<=GuardScreenSettings.HIGHLIGHT_TARGET_COLOR||i==GuardScreenSettings.HIGHLIGHT_ATTACKER_ENABLED||i==GuardScreenSettings.HIGHLIGHT_TARGET_ENABLED;
    }
    private int categoryMask(int category) {
        return switch (category) {
            case 7 -> GuardClientPreset.PARTICLES;
            case 2,24->GuardClientPreset.SCREEN;
            case 15->GuardClientPreset.ANIMATIONS;
            case 17->GuardClientPreset.HUD;
            case 14 -> GuardClientPreset.PUNCHY;
            case 3 -> GuardClientPreset.AUDIO;
            default -> 0;
        };
    }

    private void resetClientPage(int category) {
        int[] values = GuardClientPreset.DEFAULTS;
        for(int i=50;i<configDraft.extraClient.length;i++)if(category==17?GuardClientPreset.categoryOf(i)==GuardClientPreset.HUD:category==2?GuardClientPreset.categoryOf(i)==GuardClientPreset.SCREEN&&!highlightSlot(i):category==24?highlightSlot(i):false)configDraft.extraClient[i]=values[i];
        switch (category) {
            case 7 -> {
                for(int i=0;i<configDraft.extraClient.length;i++)if(GuardClientPreset.categoryOf(i)==GuardClientPreset.PARTICLES)configDraft.extraClient[i]=values[i];
                }
            case 2 -> {
                configDraft.extraClient[2]=values[2];
                configDraft.memeFlash = values[24] != 0;
                configDraft.flashStrength = values[4]; configDraft.shakeStrength = values[5];
                configDraft.hitlagTicks = values[3];
                configDraft.perfectOnlyFlash = values[27] != 0;
                configDraft.impactFrames = values[34]; configDraft.impactPerfectOnly = values[35] != 0;
                configDraft.impactBrightness = values[40]; configDraft.impactContrast = values[41];
                configDraft.impactEdges = values[42]; configDraft.impactGrain = values[43];
            }
            case 17 -> {configDraft.hud=values[0]!=0;configDraft.shieldEffects=values[1]!=0;}
            case 15 -> { configDraft.firstPersonAnimation = values[11] != 0; configDraft.thirdPersonAnimation = values[49] != 0; }
            case 3 -> { configDraft.masterVolume = values[12]; configDraft.parryVolume = values[13];
                configDraft.perfectVolume = values[14]; configDraft.blockVolume = values[15]; }
            case 14 -> {
                for(int i=0;i<configDraft.extraClient.length;i++)if(GuardClientPreset.categoryOf(i)==GuardClientPreset.PUNCHY)configDraft.extraClient[i]=values[i];
                if(poseDraft!=null){
                    poseDraft.enabled=values[65]!=0;poseDraft.poses.clear();
                    for(int i=0;i<GuardPoseSettings.COUNT;i++)poseDraft.poses.add(dev.zeli.mallardguard.GuardPoseLibrary.preset(i));
                }
            }
            case 24 -> {}
            case 4 -> {}
            default -> { return; }
        }
        dirty = dirtyClient = true;
    }

    private void resetServerPage(int category, GuardPackets.Settings d) {
        switch (category) {
            case 12 -> { setMobSettings(GuardConfig.mobSnapshot(true)); dirtyMobs = true; }
            case 0 -> {
                resetServerPage(5,d);
                configDraft.parryHealing = d.parryHealing(); configDraft.parryHealingHearts = d.parryHealingHearts();
                configDraft.parry = d.parry(); configDraft.perfect = d.perfect(); configDraft.window = d.window(); configDraft.recharge = d.recharge();
                configDraft.angle = d.angle(); configDraft.followUp = d.followUp(); configDraft.parryReturnPercent = d.parryReturnPercent();
                configDraft.perfectReturnPercent = d.perfectReturnPercent(); configDraft.retaliationCap = GuardConfig.defaultShieldSnapshot().retaliationCap(); dirtyShield = true; configDraft.knockbackStrength = d.knockbackStrength();
                configDraft.parryProjectiles = d.parryProjectiles(); configDraft.parryExplosions = d.parryExplosions();
                configDraft.perfectExplosionsOnly = d.perfectExplosionsOnly();
                GuardPackets.ShieldSettings shieldDefaults = GuardConfig.defaultShieldSnapshot();
                configDraft.retaliationEnabled=shieldDefaults.retaliationEnabled();configDraft.perfectProjectilesOnly=shieldDefaults.perfectProjectilesOnly();configDraft.perfectFallOnly=shieldDefaults.perfectFallOnly();configDraft.perfectKnockbackMultiplier=shieldDefaults.perfectKnockbackMultiplier();configDraft.incomingReaction=shieldDefaults.incomingReaction();configDraft.parriedEnabled=shieldDefaults.parriedEnabled();configDraft.parriedGuardBreakTicks=shieldDefaults.parriedGuardBreakTicks();configDraft.parriedStunTicks=shieldDefaults.parriedStunTicks();
                configDraft.toolPush = shieldDefaults.weaponPushback();
                dirtyShield = true;
            }
            case 1 -> {
                configDraft.block = d.block(); configDraft.reductionPercent = d.reductionPercent();
                configDraft.guardMovementPercent = d.guardMovementPercent(); configDraft.blockProjectiles = d.blockProjectiles();
                configDraft.blockDeflectChance = d.blockDeflectChance(); configDraft.blockExplosions = d.blockExplosions();
                GuardPackets.ShieldSettings shieldDefaults = GuardConfig.defaultShieldSnapshot();
                configDraft.toolMaxBlocks = shieldDefaults.toolMaxBlocks(); configDraft.shieldBreakTicks = shieldDefaults.breakTicks();
                dirtyShield = true;
            }
            case 2 -> { configDraft.hitParticles = d.hitParticles(); configDraft.serverHitSounds = d.hitSounds(); }
            case 4 -> {
                var hands=GuardConfig.defaultShieldSnapshot();
                configDraft.parryHandPriority=hands.parryHandPriority();configDraft.shieldParryPriority=hands.shieldParryPriority();configDraft.forceCrouchOffhand=hands.forceCrouchOffhand();
                configDraft.allowAnyItem = d.allowAnyItem(); configDraft.allowUsableItems = d.allowUsableItems();
                configDraft.consumablePriority = d.consumablePriority();
                configDraft.cooldownPreventsGuard = GuardConfig.defaultShieldSnapshot().cooldownPreventsGuard();
                configDraft.allowEmptyHand = GuardConfig.defaultShieldSnapshot().allowEmptyHand();
                dirtyShield = true;
                configDraft.parryWear = d.parryWear(); configDraft.perfectWear = d.perfectWear(); configDraft.blockWear = d.blockWear();
                configDraft.includedItems = d.includedItems(); configDraft.excludedItems = d.excludedItems();
                configDraft.itemBlockCounts=hands.itemBlockCounts();
            }
            case 5 -> {
                configDraft.fallParry = d.fallParry();
                configDraft.fallBreakBlocks = d.fallBreakBlocks(); configDraft.fallBlastStrength = d.fallBlastStrength();
                configDraft.fallLaunchPower = d.fallLaunchPower();
            }
            case 6 -> { configDraft.damageRules.clear(); dirtyDamageRules = true; }
            case 11 -> {
                int weaponPush = configDraft.toolPush, weaponBlocks = configDraft.toolMaxBlocks, sharedBreak = configDraft.shieldBreakTicks, sharedCap = configDraft.retaliationCap;
                int handRule=configDraft.parryHandPriority, shieldRule=configDraft.shieldParryPriority; boolean crouchRule=configDraft.forceCrouchOffhand, cooldownRule=configDraft.cooldownPreventsGuard, emptyRule=configDraft.allowEmptyHand, foodRule=configDraft.consumablePriority;
                String assignedCounts=configDraft.itemBlockCounts;boolean incoming=configDraft.parriedEnabled;int reaction=configDraft.incomingReaction,guardBreakTicks=configDraft.parriedGuardBreakTicks,stunTicks=configDraft.parriedStunTicks;
                boolean returnEnabled=configDraft.retaliationEnabled,projectileOnly=configDraft.perfectProjectilesOnly,fallOnly=configDraft.perfectFallOnly;int perfectPush=configDraft.perfectKnockbackMultiplier;
                setShieldSettings(GuardConfig.defaultShieldSnapshot());
                configDraft.retaliationEnabled=returnEnabled;configDraft.perfectProjectilesOnly=projectileOnly;configDraft.perfectFallOnly=fallOnly;configDraft.perfectKnockbackMultiplier=perfectPush;
                configDraft.itemBlockCounts=assignedCounts;configDraft.incomingReaction=reaction;configDraft.parriedEnabled=incoming;configDraft.parriedGuardBreakTicks=guardBreakTicks;configDraft.parriedStunTicks=stunTicks;
                configDraft.toolPush = weaponPush; configDraft.toolMaxBlocks = weaponBlocks; configDraft.shieldBreakTicks = sharedBreak; configDraft.retaliationCap = sharedCap;
                configDraft.parryHandPriority=handRule;configDraft.shieldParryPriority=shieldRule;configDraft.forceCrouchOffhand=crouchRule;configDraft.cooldownPreventsGuard=cooldownRule; configDraft.allowEmptyHand = emptyRule; configDraft.consumablePriority = foodRule;
                configDraft.shieldItems = d.shieldItems(); configDraft.shieldBlacklist=GuardConfig.SHIELD_BLACKLIST.getDefault(); dirtyShield = true;
            }
            default -> { return; }
        }
        dirty = dirtyServer = true;
        if (category != 6 && category != 12) dirtyServerRules = true;
    }

    private void save(boolean close) {
        if (pendingSave != null) return;
        if (!hasChanges()) { dirty = false; return; }

        if (operator && (!GuardItemRules.valid(configDraft.includedItems) || !GuardItemRules.valid(configDraft.excludedItems) || !GuardItemRules.valid(configDraft.shieldItems) || !GuardItemRules.valid(configDraft.shieldBlacklist) || !dev.zeli.mallardguard.GuardItemBlockCounts.valid(configDraft.itemBlockCounts)
            || !GuardItemRules.validEntityRules(configDraft.stunnableBosses))) {
            configDraft.invalidItemRules = true;
            experimentalSection = false;
            enforceSection = false;
            clientSection = false;
            page = !GuardItemRules.valid(configDraft.shieldItems) || !GuardItemRules.valid(configDraft.shieldBlacklist) || !dev.zeli.mallardguard.GuardItemBlockCounts.valid(configDraft.itemBlockCounts) || !GuardItemRules.validEntityRules(configDraft.stunnableBosses) ? 11 : 4;
            eligibilityOpen = true;
            firstRow = 0;
            rebuildWidgets();
            if (page == 4) scrollTo(2);
            else {shieldIdsOpen=true;rememberedGroups.put(groupKey(),"Additional Shields and Bosses");selectRememberedGroup();rebuildWidgets();}
            return;
        }
        if (operator && (!GuardItemRules.validEntityIds(configDraft.mobWhitelist)||!GuardItemRules.valid(configDraft.mobGearWhitelist)||!GuardItemRules.valid(configDraft.mobGearBlacklist))) {
            experimentalSection = true; enforceSection = clientSection = false; page = 12;
            rememberedGroups.put(groupKey(), "Mob Eligibility"); selectRememberedGroup(); firstRow = 0; rebuildWidgets();
            GuardClient.configMessage(Component.literal("Use exact entity IDs in Mob Eligibility and valid item rules in Mob Gear."));
            return;
        }
        if(operator&&dirtyPolicy&&!configDraft.enforcedPoseData.isEmpty()&&dev.zeli.mallardguard.GuardPoseLibrary.parseAnimations(configDraft.enforcedPoseData)==null){GuardClient.configMessage(Component.literal("Mallard Guard: The enforced animation assignments are too large to save."));return;}
        try(GuardConfig.ClientEdit edit=new GuardConfig.ClientEdit()) {
            GuardConfig.DEBUG_CLIENT_FLAGS.set(configDraft.debugFlags);
            GuardDiagnostics.event(GuardDiagnostics.CONFIG,"Applying config changes: client="+dirtyClient+", server="+dirtyServer+", enforce="+dirtyPolicy);
            int[] clientValues = configDraft.extraClient.clone();
            clientValues[0] = (configDraft.hud) ? 1 : 0;
            clientValues[1] = (configDraft.shieldEffects) ? 1 : 0;
            clientValues[2]=configDraft.extraClient[2];
            clientValues[3] = configDraft.hitlagTicks;
            clientValues[4] = configDraft.flashStrength;
            clientValues[5] = configDraft.shakeStrength;
            clientValues[11] = configDraft.firstPersonAnimation ? 1 : 0;
            clientValues[12] = configDraft.masterVolume;
            clientValues[13] = configDraft.parryVolume;
            clientValues[14] = configDraft.perfectVolume;
            clientValues[15] = configDraft.blockVolume;
            clientValues[24] = (configDraft.memeFlash) ? 1 : 0;
            clientValues[27] = (configDraft.perfectOnlyFlash) ? 1 : 0;
            clientValues[34] = configDraft.impactFrames;
            clientValues[35] = (configDraft.impactPerfectOnly) ? 1 : 0;
            clientValues[40] = configDraft.impactBrightness;
            clientValues[41] = configDraft.impactContrast;
            clientValues[42] = configDraft.impactEdges;
            clientValues[43] = configDraft.impactGrain;
            clientValues[49] = (configDraft.thirdPersonAnimation) ? 1 : 0;
            clientValues[50] = configDraft.extraClient[50];
            clientValues[51] = configDraft.extraClient[51];
            clientValues[52] = configDraft.extraClient[52];
            clientValues[53] = configDraft.extraClient[53];
            clientValues[54] = configDraft.extraClient[54];
            clientValues[55] = configDraft.extraClient[55];
            clientValues[56] = (configDraft.extraClient[56] != 0) ? 1 : 0;
            clientValues[57] = (configDraft.extraClient[57] != 0) ? 1 : 0;
            clientValues[58] = configDraft.extraClient[58];
            clientValues[59] = configDraft.extraClient[59];
            clientValues[60] = (configDraft.extraClient[60] != 0) ? 1 : 0;
            clientValues[61] = (configDraft.extraClient[61] != 0) ? 1 : 0;
            clientValues[62] = configDraft.extraClient[62];
            clientValues[6] = configDraft.extraClient[6];
            clientValues[32] = configDraft.extraClient[32];
            clientValues[63] = configDraft.extraClient[63];
            clientValues[64] = configDraft.extraClient[64];
            clientValues[47] = GuardParticleConfig.PRESET_REVISION;
            for (int category : GuardClientPreset.CATEGORIES) {
                if (category != GuardClientPreset.PUNCHY) GuardClientPreset.apply(clientValues, category);
            }
            if (!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) { GuardPoseSettings.apply(clientValues); }
            if (dirtyClient) {
                if (!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) {
                    if (!dev.zeli.mallardguard.GuardPoseLibrary.save(poseDraft.poses)) { GuardClient.configMessage(Component.literal("Mallard Guard poses could not be saved. Please check the game log.")); return; }
                }
                GuardClient.saveClientConfig();
            }
            edit.commit();
        }catch(RuntimeException error){
            GuardClient.configMessage(Component.literal("Mallard Guard: Client settings could not be saved. Previous values were restored. Check the game log."));
            org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Client config save failed",error);
            return;
        }
        GuardPackets.Save rulesSave = operator && dirtyServerRules ? new GuardPackets.Save(configDraft.parry, configDraft.block, configDraft.parryDrowningFire, configDraft.parryStarvation, configDraft.parryGenericKill, configDraft.perfect, configDraft.window, configDraft.recharge, configDraft.angle, configDraft.reductionPercent, configDraft.followUp, configDraft.parryReturnPercent, configDraft.perfectReturnPercent, configDraft.parryWear, configDraft.perfectWear, configDraft.blockWear, configDraft.serverHitSounds, configDraft.hitParticles, configDraft.serverMasterVolume, configDraft.serverPerfectVolume, configDraft.serverParryVolume, configDraft.serverBlockVolume, configDraft.fallParry, true, true, configDraft.fallBreakBlocks, configDraft.fallBlastStrength, configDraft.fallLaunchPower, configDraft.parryExplosions, configDraft.perfectExplosionsOnly, configDraft.blockExplosions, configDraft.parryProjectiles, configDraft.blockProjectiles, configDraft.knockbackStrength > 0, configDraft.knockbackStrength, configDraft.guardMovementPercent, configDraft.blockDeflectChance, configDraft.allowAnyItem, configDraft.allowUsableItems, configDraft.includedItems, configDraft.excludedItems, configDraft.shieldItems, configDraft.consumablePriority, configDraft.parryHealing, configDraft.parryHealingHearts) : null;
        GuardPackets.SaveMobs mobsSave = operator && dirtyMobs ? new GuardPackets.SaveMobs(new GuardPackets.MobSettings(
            configDraft.mobGuard,configDraft.mobDifficulty,configDraft.mobWhitelist,configDraft.mobGearChance,configDraft.mobGearWhitelist,configDraft.mobGearBlacklist,configDraft.mobFlyingColor,configDraft.mobDebrisColor,configDraft.mobTracerStartColor,configDraft.mobTracerMiddleColor,configDraft.mobTracerEndColor)) : null;
        if (operator && dirtyShield) configDraft.shieldBreakTicks = Math.max(configDraft.recharge, configDraft.shieldBreakTicks);
        GuardPackets.SaveShield shieldSave = operator && dirtyShield ? new GuardPackets.SaveShield(new GuardPackets.ShieldSettings(
            configDraft.shieldPerfect, configDraft.shieldWindow, configDraft.shieldRechargeTicks, configDraft.shieldMaxBlocks, configDraft.toolMaxBlocks, Math.max(configDraft.recharge, configDraft.shieldBreakTicks), configDraft.shieldCone, configDraft.shieldReach, configDraft.shieldReturn,
            configDraft.shieldStun, configDraft.shieldPerfectPush, configDraft.shieldRegularPush, configDraft.toolPush, configDraft.stunnableBosses, configDraft.consumablePriority, configDraft.cooldownPreventsGuard, configDraft.allowEmptyHand, configDraft.retaliationCap, configDraft.shieldParryPriority, configDraft.parryHandPriority, configDraft.forceCrouchOffhand, configDraft.shieldBlacklist, configDraft.itemBlockCounts, GuardClient.currentShieldSettings().shieldExpansionActive(),configDraft.parriedEnabled,configDraft.parriedGuardBreakTicks,configDraft.parriedStunTicks,configDraft.shieldConeEnabled,configDraft.incomingReaction,configDraft.retaliationEnabled,configDraft.perfectProjectilesOnly,configDraft.perfectFallOnly,configDraft.perfectKnockbackMultiplier)) : null;
        GuardPackets.SaveDamageRules damageSave = operator && dirtyDamageRules ? new GuardPackets.SaveDamageRules(
            GuardDamageRules.encodeRules(configDraft.damageRules)) : null;
        GuardPackets.SaveClientPolicy policySave = operator && dirtyPolicy ? new GuardPackets.SaveClientPolicy(configDraft.enforceEnabled, configDraft.dontEnforceMask,
            GuardClientPreset.encode(configDraft.enforcedDefaults),configDraft.enforcedPoseData) : null;
        GuardPackets.SaveConfig request = new GuardPackets.SaveConfig(++nextSaveRequest, rulesSave, mobsSave, shieldSave, damageSave, policySave);
        SavedState submitted = captureState();
        boolean clientSaved = dirtyClient;
        if (request.mask() != 0) {
            if (Minecraft.getInstance().getConnection() == null) {
                acceptSavedValues(submitted, 0, 0, clientSaved);
                dirtyClient = false;
                dirty = hasChanges();
                GuardClient.configMessage(Component.literal("Mallard Guard: Connect to a server or world to save server settings."));
                return;
            }
            pendingSave = new PendingSave(request.request(), request.mask(), submitted, System.nanoTime(), clientSaved, close);
            GuardConfigTransfer.sendConfig(request,Minecraft.getInstance().getConnection().registryAccess());
            return;
        }
        if (clientSaved) GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the Client."));
        acceptSavedValues(submitted, 0, 0, clientSaved);
        dirtyClient = false;
        dirty = hasChanges();
        if (close && !dirty) onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (settingsSearchChanged) {
            settingsSearchChanged = false;
            rebuildWidgets();
            if (settingsSearchBox != null) {
                setFocused(settingsSearchBox);
                settingsSearchBox.setFocused(true);
                settingsSearchBox.setCursorPosition(settingsSearch.length());
            }
        }
        updateRowLayouts();
        // Blur the game world before drawing either surface or any controls.
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0x3219141E);
        GuardUi.panel(graphics,sidebarLeft-6,layoutTop-5,sidebarWidth+13,height-layoutTop,true);
        GuardUi.panel(graphics,panelLeft-8,layoutTop-5,panelWidth+22,height-layoutTop,false);
        graphics.fill(contentRight() + 4, rowTop(), contentRight() + 5,
            rowTop() + visibleRows * ROW_STEP - 7, 0xFF76647F);
        for (DropdownLabel dropdown : dropdownLabels) {
            boolean hover = mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22;
            if (dropdown.enabled()&&(hover || dropdown.expanded().getAsBoolean())) graphics.fill(panelLeft, dropdown.y(), contentRight(), dropdown.y() + 22,
                hover ? 0x88493E55 : 0x49493E55);
            if (dropdown.enabled()&&dropdown.expanded().getAsBoolean()) graphics.fill(panelLeft, dropdown.y(), panelLeft + 2, dropdown.y() + 22, WARM_EDGE);
            int dropdownColor = !dropdown.enabled() ? 0xFF888888 : WARM_EDGE;
            int titleRight=contentRight()-18;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==dropdown.y())titleRight=Math.min(titleRight,control.getX()-4);
            graphics.drawString(font,font.plainSubstrByWidth(dropdown.name(),Math.max(0,titleRight-panelLeft-4)),panelLeft+4,dropdown.y()+3,dropdownColor);
            int lineEnd=contentRight()-4;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==dropdown.y())lineEnd=Math.min(lineEnd,control.getX()-4);
            graphics.fill(panelLeft + 4, dropdown.y() + 18, lineEnd,
                dropdown.y() + 19, !dropdown.enabled() ? 0x88777777 : GuardUi.WARM);
            GuardUi.chevron(graphics,contentRight()-GuardUiLayout.ARROW_SIZE-GuardUiLayout.CONTROL_GAP,
                dropdown.y()+(22-GuardUiLayout.ARROW_SIZE)/2,dropdown.expanded().getAsBoolean()?1:0,dropdownColor);
        }
        for (SettingLabel section : sectionLabels) {
            int lineEnd=contentRight()-4;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==section.y())lineEnd=Math.min(lineEnd,control.getX()-4);
            graphics.drawString(font,font.plainSubstrByWidth(section.name(),Math.max(0,lineEnd-panelLeft-4)),panelLeft+4,section.y()+4,WARM_EDGE);
            graphics.fill(panelLeft + 4, section.y() + 18, lineEnd, section.y() + 19, WARM_EDGE);
        }
        // Keep the panel and every widget on the same render layer.
        for (var widget : renderables) if (!(widget instanceof Button)) widget.render(graphics, mouseX, mouseY, delta);
        for (Button button : themedButtons) paintButton(graphics, button, mouseX, mouseY);
        for(ColorPreview preview:colorPreviews){Button swatch=preview.button();if(swatch.visible)graphics.fill(swatch.getX()+4,swatch.getY()+4,swatch.getX()+swatch.getWidth()-4,swatch.getY()+16,0xFF000000|preview.color().getAsInt());}
        // Shared geometry and colors also own hover/drag behavior.
        GuardUi.scrollbar(graphics,scrollbar(),mouseX,mouseY,draggingScroll);
        if (experimentalSection && page == 14 && !PunchyGuardCompat.installed()) {
            graphics.drawWordWrap(font, Component.literal("Punchy is required to access these settings. Install Punchy to create or edit guard poses."), panelLeft + 8, rowTop() + 12, panelWidth - 30, GuardUi.TEXT);
        }
        graphics.drawString(font,font.plainSubstrByWidth(title.getString(),sidebarWidth-84),sidebarLeft+33,layoutTop+8,TEXT);
        graphics.drawString(font, "v" + GuardConfig.version(), sidebarLeft+33, layoutTop + 21, 0xFFC3B4CA);
        int dividerX=(sidebarLeft+sidebarWidth+7+panelLeft-8)/2;
        graphics.fill(dividerX,layoutTop-5+GuardUiLayout.DIVIDER_INSET,dividerX+1,
            height-5-GuardUiLayout.DIVIDER_INSET,GuardUiLayout.DIVIDER_COLOR);
        if (settingsSearch.isBlank()) {
            int[] categories = enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = sidebarOffset; i < Math.min(categories.length, sidebarOffset + visibleSidebarTabs(categories.length)); i++) {
                if (page == categories[i]) graphics.fill(sidebarLeft + 2, sidebarTop() + (i - sidebarOffset) * sidebarStep(categories.length) + 3,
                    sidebarLeft + 5, sidebarTop() + (i - sidebarOffset) * sidebarStep(categories.length) + sidebarStep(categories.length)-4, GuardUi.WARM);
            }
        }
        for(var entry:rowValues.entrySet()){String old=originalRows.get(entry.getKey()),value=entry.getValue().get();if(old!=null&&!old.equals(value)){
            changedRows.put(entry.getKey(),"Saved: "+old+" → Current: "+value);
            graphics.fill(panelLeft-5,row(entry.getKey())+7,panelLeft-2,row(entry.getKey())+10,GuardUi.HIGHLIGHT);
        }else changedRows.remove(entry.getKey());}
        for (SettingLabel setting : settingLabels) {
            String name = setting.name();
            int limit = settingX() - panelLeft - 8;
            while (font.width(name) > limit && name.length() > 1) name = name.substring(0, name.length() - 2) + "…";
            int textColor = setting.enabled() ? TEXT : 0xFF8B8190;
            int separator = setting.name().indexOf(" | ");
            if (!clientSection && page == 6 && separator > 0) {
                String id = setting.name().substring(separator + 3);
                textColor = configDraft.damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id)) ? 0xB6E8B6 : 0xA7A7A7;
            }
            int rowIndex=firstRow+(setting.y()-rowTop())/ROW_STEP;
            boolean changed=changedRows.containsKey(rowIndex);
            graphics.drawString(font,name,panelLeft,setting.y()+(changed?1:6),changed?GuardUi.HIGHLIGHT:textColor);
            if(changed){String savedValue="Saved: "+originalRows.get(rowIndex);graphics.drawString(font,font.plainSubstrByWidth(savedValue,limit),panelLeft,setting.y()+11,0xFFAFA1B6);}

        }
        if (!clientSection && configDraft.invalidItemRules) {
            graphics.drawString(font, "Check the item IDs and commas.", panelLeft, rowTop() - 9, 0xFF6666);
        }
        if (mouseX >= panelLeft && mouseX < settingX() - 5) {
            for (SettingLabel setting : settingLabels) {
                if (mouseY >= setting.y() && mouseY < setting.y() + 20) {
                    graphics.renderTooltip(font, font.split(Component.literal(setting.description()+(changedRows.containsKey(firstRow+(setting.y()-rowTop())/ROW_STEP)?"\n"+changedRows.get(firstRow+(setting.y()-rowTop())/ROW_STEP):"")), Math.min(320, width - 24)), mouseX, mouseY);
                    break;
                }
            }
        }
        for (DropdownLabel dropdown : dropdownLabels) {
            if (mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22
                && themedButtons.stream().noneMatch(b->b.visible&&b.isMouseOver(mouseX,mouseY))) {
                graphics.renderTooltip(font, font.split(Component.literal(dropdown.description()), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (Button button : themedButtons) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, font.split(Component.literal(buttonTips.get(button)), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (var entry : widgetTips.entrySet()) if (!(entry.getKey() instanceof Button)
            && entry.getKey().isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.literal(entry.getValue()), Math.min(320, width - 24)), mouseX, mouseY);
            break;
        }
        if (pendingSave != null) {
            graphics.fill(0, 0, width, height, 0x991A1520);
            graphics.drawCenteredString(font, Component.literal("Waiting for the server to save settings…"), width / 2, height / 2, 0xFFFFFF);
        }
    }

    private void paintButton(GuiGraphics graphics,Button button,int mouseX,int mouseY){
        if(button==logoButton||button==reportButton||rowUndoButtons.containsValue(button)){button.render(graphics,mouseX,mouseY,0);return;}
        boolean selected=!button.active&&buttonTips.getOrDefault(button,"").startsWith("Open ");
        GuardUi.paint(graphics,button,mouseX,mouseY,selected,redButtons.contains(button));
    }

    private void scrollTo(int row) {
        int next = Math.clamp(row, 0, maxScroll());
        if (next != firstRow) {
            firstRow = next;
            rememberLocation();
            rebuildWidgets();
        }
    }

    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(contentRight()+8,rowTop(),visibleRows*ROW_STEP-7,visibleRows,pageRows(),firstRow);}
    private void scrollAt(double mouseY){scrollTo(scrollbar().scrollAt(mouseY,scrollGrab));}

    private void selectSection(int section) {
        rememberLocation();
        settingsSearch = "";
        settingsSearchChanged = false;
        if (experimentalSection) { experimentalPage = page; experimentalRow = firstRow; }
        else if (enforceSection) {enforceRow=firstRow;enforcePage=page;}
        else if (clientSection) { clientPage = page; clientRow = firstRow; }
        else { serverPage = page; serverRow = firstRow; }
        experimentalSection = section == 3;
        enforceSection = section == 2;
        clientSection = section == 0;
        page = experimentalSection ? experimentalPage : enforceSection ? enforcePage : clientSection ? clientPage : serverPage;
        if (!enforceSection) {
            int[] categories = enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            sidebarOffset = 0;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        selectRememberedGroup();
        rememberLocation();
        if(Minecraft.getInstance().screen==this)rebuildWidgets();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pendingSave != null) return true;
        for(var child:children())if(child instanceof GuardSlider slider && slider.active && slider.visible && slider.isMouseOver(mouseX,mouseY))
            return slider.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth + 18
            && mouseY >= rowTop() && mouseY < rowTop() + visibleRows * ROW_STEP && maxScroll() > 0) {
            scrollTo(firstRow - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingSave != null) return true;

        if (button == 0 && scrollbar().contains(mouseX,mouseY)) {
            draggingScroll = true;scrollGrab=scrollbar().grab(mouseY);
            scrollAt(mouseY);
            return true;
        }
        if (button == 0 && enforceSection && !configDraft.enforceEnabled && mouseX >= panelLeft && mouseX < contentRight())
            return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= panelLeft && mouseX < contentRight()) {
            for (DropdownLabel dropdown : dropdownLabels) {
                if (mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                    for(Button control:themedButtons)if(control.active&&control.isMouseOver(mouseX,mouseY))return super.mouseClicked(mouseX,mouseY,button);
                    if(!dropdown.enabled())return true;
                    if(java.util.Arrays.asList(GuardShieldReactions.NAMES).contains(dropdown.name()))reactionGroup=dropdown.expanded().getAsBoolean()?"":dropdown.name();
                    else rememberedGroups.put(groupKey(), dropdown.expanded().getAsBoolean() ? "" : dropdown.name());
                    selectRememberedGroup();
                    firstRow = 0;
                    rememberLocation();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean charTyped(char character, int modifiers) {
        return pendingSave != null || super.charTyped(character, modifiers);
    }

    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if (pendingSave != null) return true;
        if(getFocused() instanceof AbstractSliderButton){boolean handled=super.keyPressed(key,scan,modifiers);return handled;}
        return super.keyPressed(key,scan,modifiers);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pendingSave != null) return true;
        if (draggingScroll && button == 0) { scrollAt(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pendingSave != null) return true;

        if (draggingScroll && button == 0) { draggingScroll = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void onClose() {
        if (pendingSave != null) return;
        rememberLocation();
        if (!hasChanges()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        GuardUi.choices(this,"Unsaved changes","Leave without saving your Mallard Guard settings?",new GuardUi.Choice("Discard Changes",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
    }

    private record SettingLabel(String name, String description, int y, boolean enabled) {}
    private record DropdownLabel(String name, int y, BooleanSupplier expanded, String description,boolean enabled) {}

    @Override public void removed(){draggingScroll=false;super.removed();}

}
