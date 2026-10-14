package dev.zeli.mallardguard;

import static dev.zeli.mallardguard.GuardClientPreset.DEFAULTS;
import static dev.zeli.mallardguard.GuardClientPreset.MIN;
import static dev.zeli.mallardguard.GuardClientPreset.MAX;

/** Read historical preset lengths/scales while preserving current reserved positions. */
final class GuardPresetMigration {
    private GuardPresetMigration() {}
    static int clampLegacy(int index, int value) { return Math.clamp(value, MIN[index], MAX[index]); }
    static int[] parse(String text) {
        if (text == null || text.length()>8192) return null;
        String[] parts = text.split(",", -1);
        // Accept saved server presets from earlier config revisions, filling new slots with defaults.
        if (parts.length != GuardScreenSettings.V155H_LENGTH && parts.length != GuardScreenSettings.V155H_LENGTH+2 && parts.length != GuardScreenSettings.BETA7_LENGTH && parts.length != GuardScreenSettings.BETA5_LENGTH && parts.length != GuardScreenSettings.BETA2_LENGTH && parts.length != GuardScreenSettings.PREVIOUS_LENGTH && parts.length != GuardParticleColors.PALETTE_PREVIOUS_LENGTH && parts.length != GuardParticleColors.LENGTH && parts.length != GuardParticleColors.CONTROLS_PREVIOUS_LENGTH && parts.length != GuardParticleColors.CONTROLS_PREVIOUS_LENGTH+2 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_51 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_51+2 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_49 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_49+2 && parts.length != GuardParticleColors.LEGACY_LENGTH && parts.length != GuardParticleColors.LEGACY_LENGTH+2 && parts.length != GuardParticleColors.LIFETIME_LENGTH && parts.length != GuardParticleColors.LIFETIME_LENGTH+2 && parts.length != GuardParticleColors.APPEARANCE_LENGTH && parts.length != GuardParticleColors.APPEARANCE_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_COMPONENT_LENGTH && parts.length != GuardParticleColors.IMPACT_COMPONENT_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_SETTINGS_LENGTH && parts.length != GuardParticleColors.IMPACT_SETTINGS_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_PREVIOUS_LENGTH && parts.length != GuardParticleColors.IMPACT_PREVIOUS_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_BASE_LENGTH && parts.length != GuardParticleColors.IMPACT_BASE_LENGTH+2 && parts.length != GuardParticleColors.PREVIOUS_LENGTH && parts.length != GuardParticleColors.OFFSET+2 && parts.length != GuardParticleColors.OFFSET+4 && parts.length != GuardParticleColors.OFFSET && parts.length != GuardSparkTracerConfig.TUNING_LENGTH && parts.length != GuardSparkTracerConfig.TUNING_LENGTH+2 && parts.length != GuardSparkTracerConfig.VARIANCE_LENGTH && parts.length != GuardSparkTracerConfig.VARIANCE_LENGTH+2 && parts.length != GuardSparkTracerConfig.SHRINK_LENGTH && parts.length != GuardSparkTracerConfig.SHRINK_LENGTH+2 && parts.length != GuardSparkTracerConfig.PREVIOUS_LENGTH && parts.length != GuardSparkTracerConfig.LEGACY_LENGTH && parts.length != GuardSparkTracerConfig.LEGACY_LENGTH+2 && parts.length != GuardSparkTracerConfig.OFFSET && parts.length != GuardSparkTracerConfig.OFFSET+2 && parts.length != DEFAULTS.length && parts.length != DEFAULTS.length+2 && parts.length != 216 && parts.length != 214 && parts.length != 217 && parts.length != GuardPoseSettings.RELEASE_DELAY_SLOT+1 && parts.length != GuardPoseSettings.RELEASE_DELAY_SLOT && parts.length != GuardShieldReactions.OFFSET && parts.length != 158 && parts.length != 186 && parts.length != 65 && parts.length != 50 && parts.length != 49 && parts.length != 48 && parts.length != 47 && parts.length != 46 && parts.length != 45 && parts.length != 44 && parts.length != 40 && parts.length != 39 && parts.length != 35 && parts.length != 29 && parts.length != 26 && parts.length != 25) return null;
        int[] values = DEFAULTS.clone();
        try {
            boolean oldScale = parts.length < 48 || Integer.parseInt(parts[47]) < 15;
            int suppliedLength=parts.length;
            if(parts.length==GuardScreenSettings.V155H_LENGTH+2 && Integer.parseInt(parts[47])<95)suppliedLength=GuardScreenSettings.V155H_LENGTH;
            else if(parts.length==GuardParticleColors.CONTROLS_PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<88)suppliedLength=GuardParticleColors.CONTROLS_PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.SERIALIZED_LENGTH_51+2 && Integer.parseInt(parts[47])<84)suppliedLength=GuardParticleColors.SERIALIZED_LENGTH_51;
            else if(parts.length==GuardParticleColors.SERIALIZED_LENGTH_49+2 && Integer.parseInt(parts[47])<83)suppliedLength=GuardParticleColors.SERIALIZED_LENGTH_49;
            else if(parts.length==GuardParticleColors.LEGACY_LENGTH+2 && Integer.parseInt(parts[47])<82)suppliedLength=GuardParticleColors.LEGACY_LENGTH;
            else if(parts.length==GuardParticleColors.LIFETIME_LENGTH+2 && Integer.parseInt(parts[47])<81)suppliedLength=GuardParticleColors.LIFETIME_LENGTH;
            else if(parts.length==GuardParticleColors.APPEARANCE_LENGTH+2 && Integer.parseInt(parts[47])<80)suppliedLength=GuardParticleColors.APPEARANCE_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_COMPONENT_LENGTH+2 && Integer.parseInt(parts[47])<79)suppliedLength=GuardParticleColors.IMPACT_COMPONENT_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_SETTINGS_LENGTH+2 && Integer.parseInt(parts[47])<78)suppliedLength=GuardParticleColors.IMPACT_SETTINGS_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<77)suppliedLength=GuardParticleColors.IMPACT_PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_BASE_LENGTH+2)suppliedLength=GuardParticleColors.IMPACT_BASE_LENGTH;
            else if(parts.length==GuardParticleColors.PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<75)suppliedLength=GuardParticleColors.PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.OFFSET+4 && Integer.parseInt(parts[47])<71)suppliedLength=GuardParticleColors.OFFSET+2;
            else if(parts.length==GuardParticleColors.OFFSET+2 && Integer.parseInt(parts[47])<70)suppliedLength=GuardParticleColors.OFFSET;
            else if(parts.length==GuardSparkTracerConfig.TUNING_LENGTH+2 && Integer.parseInt(parts[47])<69)suppliedLength=GuardSparkTracerConfig.TUNING_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.VARIANCE_LENGTH+2)suppliedLength=GuardSparkTracerConfig.VARIANCE_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.SHRINK_LENGTH+2)suppliedLength=GuardSparkTracerConfig.SHRINK_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<66)suppliedLength=GuardSparkTracerConfig.PREVIOUS_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.LEGACY_LENGTH+2)suppliedLength=GuardSparkTracerConfig.LEGACY_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.OFFSET+2)suppliedLength=GuardSparkTracerConfig.OFFSET;
            else if(parts.length==186)suppliedLength=GuardShieldReactions.OFFSET;
            for (int i = 0; i < Math.min(suppliedLength,values.length); i++) {
                if(GuardParticleColors.retired(i) || GuardSparkTracerConfig.retired(i) || GuardClientSettings.reserved(i) || GuardScreenSettings.retired(i)) { values[i]=DEFAULTS[i];continue; }
                if(GuardScreenSettings.slot(i)){values[i]=Integer.parseInt(parts[i]);if(parts.length<48||Integer.parseInt(parts[47])<90){if(i==GuardScreenSettings.HIGHLIGHT_FADE||i==GuardScreenSettings.HIGHLIGHT_DURATION)values[i]=Math.clamp((int)Math.round(values[i]/50.0),GuardScreenSettings.min(i),GuardScreenSettings.max(i));if(i==GuardScreenSettings.HITLAG_PERFECT_ONLY)values[i]=values[i]==0?1:0;}if(values[i]<GuardScreenSettings.min(i)||values[i]>GuardScreenSettings.max(i))return null;continue;}
                if(i==47 && Integer.parseInt(parts[i])<GuardParticleConfig.PRESET_REVISION){values[i]=DEFAULTS[i];continue;}
                if (i>=16 && i<=21 && (parts.length<48 || Integer.parseInt(parts[47])<53) && (i>=19 || parts.length<48 || Integer.parseInt(parts[47])<52)) { values[i] = DEFAULTS[i]; continue; }
                values[i] = Integer.parseInt(parts[i]);
                if(i>=GuardPoseSettings.OFFSET && i<GuardPoseSettings.OFFSET+GuardPoseSettings.SIZE && (i-GuardPoseSettings.OFFSET)%GuardPoseSettings.STRIDE>=19 && (i-GuardPoseSettings.OFFSET)%GuardPoseSettings.STRIDE<=20 && parts.length>=48 && Integer.parseInt(parts[47])<73){
                    if(values[i]<0 || values[i]>2000)return null;values[i]=Math.min(500,values[i]);
                }
                if ((i==3||i==62)&&(parts.length<48||Integer.parseInt(parts[47])<90))values[i]=(int)Math.round(values[i]/3.0);
                if (i == 12) values[i] = Math.min(200, values[i]);
                if (i == 34) values[i] = Math.min(1, values[i]);
                if (parts.length < 48) values[i] = clampLegacy(i, values[i]);
                if (oldScale && i < 65) values[i] = clampLegacy(i, values[i]);
                if(i==GuardParticleColors.IMPACT_DURATION && Integer.parseInt(parts[47])<77){
                    if(values[i]<25||values[i]>400)return null;
                    values[i]=GuardParticleColors.durationFromSpeed(values[i]);
                }
                if(i==GuardParticleColors.TRACER_TRAIL_TICKS && Integer.parseInt(parts[47])<80){if(values[i]<1||values[i]>20)return null;values[i]=Math.min(4,values[i]);}
                if(i==GuardParticleColors.IMPACT_SPIN && Integer.parseInt(parts[47])<82){if(values[i]<-720||values[i]>720)return null;values[i]=Math.abs(values[i]);}
                if(GuardParticleColors.slot(i)){if(!GuardParticleColors.valid(i,values[i]))return null;continue;}
                if(GuardSparkTracerConfig.slot(i)){if(!GuardSparkTracerConfig.valid(i,values[i]))return null;continue;}
                if(i==GuardPoseSettings.RELEASE_DELAY_SLOT){if(values[i]<0||values[i]>1000)return null;continue;}
                if(i>=GuardShieldReactions.OFFSET){int r=i-GuardShieldReactions.OFFSET;if(values[i]<(r%6==5?1:0)||values[i]>GuardShieldReactions.max(r))return null;continue;}
                if (i >= 65 ? values[i] < (i == 65 ? 0 : GuardPoseSettings.min((i - 66) % GuardPoseSettings.STRIDE)) || values[i] > (i == 65 ? 1 : GuardPoseSettings.max((i - 66) % GuardPoseSettings.STRIDE)) : values[i] < MIN[i] || values[i] > MAX[i]) return null;
            }
            if(suppliedLength>GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.INTENSITY && Integer.parseInt(parts[47])<69) {
                int intensity=Integer.parseInt(parts[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.INTENSITY]);
                int enabled=Integer.parseInt(parts[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.ENABLED]);
                if(intensity<0||intensity>100||enabled<0||enabled>1)return null;
                GuardSparkTracerConfig.migrateAmounts(values,intensity,enabled!=0);
            }
            if(parts.length>=48&&Integer.parseInt(parts[47])<70){
                GuardParticleConfig.migratePerfect(values);
                int revision=Integer.parseInt(parts[47]);
                if(suppliedLength<=17 || revision<52)values[17]=DEFAULTS[17];
                if(suppliedLength<=20 || revision<53)values[20]=DEFAULTS[20];
                if(suppliedLength<=GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.BASEINTENSITY)values[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY]=DEFAULTS[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY];
            }
        } catch (NumberFormatException | ArithmeticException error) { return null; }
        if(parts.length>28 && (parts.length<48 || Integer.parseInt(parts[47])<88))values[62]="0".equals(parts[28])?values[3]:0;
        return values;
    }
}
