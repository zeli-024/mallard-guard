package dev.zeli.mallardguard;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Compact, bounded pose values shared by local settings and server client presets. */
public final class GuardPoseSettings {
    public static final int COUNT = 5, STRIDE = 23, OFFSET = 66, SIZE = COUNT * STRIDE;
    public static final int RELEASE_DELAY_SLOT=GuardShieldReactions.OFFSET+GuardShieldReactions.SIZE;
    public static int[] appendMotion(int[] values,boolean local){int[] result=Arrays.copyOf(values,RELEASE_DELAY_SLOT+1);result[RELEASE_DELAY_SLOT]=local?GuardConfig.PUNCHY_RELEASE_DELAY.get():50;return GuardParticleConfig.appendParticleSettings(result,local);}
    private GuardPoseSettings() {}
    private static final int[][] PRESETS = {
        {1, 17, 13, -28, 7, -14, 74, 10, 25, -16, 23, 30, -18, 0, 0, 0, 36, -7, -19, 150, 360, 2, 150},
        {1, 91, 10, -30, 22, -6, -10, -5, 5, 3, 10, -8, -18, 0, 0, 0, 55, 9, 13, 150, 360, 2, 150},
        {1, 8, 100, -12, -23, 11, 54, -5, 8, 3, 10, -8, -18, 0, 0, 0, 38, -29, 11, 150, 360, 2, 150},
        {1, 68, 19, -24, 4, -33, 80, -5, 8, 3, 10, -8, -18, 0, 0, 0, 24, 180, 10, 150, 360, 3, 100},
        {1, 39, 3, -34, -39, -10, 43, -26, 24, 45, -54, -11, -66, 0, 0, 0, 0, -8, 10, 150, 360, 2, 100}
    };
    public static int[] defaultPose(int index) { return PRESETS[index].clone(); }
    /** Independent editable copy; no recoil offset is added automatically. */
    public static int[] defaultParriedPose(int[] guarding) {
        return guarding.clone();
    }
    public static int min(int field) {
        if (field == 0 || field >= 19) return 0;
        return (field >= 4 && field <= 6 || field >= 10 && field <= 12 || field >= 16 && field <= 18) ? -360 : -2000;
    }
    public static int max(int field) {
        if (field == 0) return 1;
        if (field == 19 || field == 20) return 500;
        if (field == 21) return 3;
        if (field == 22) return 200;
        return min(field) == -360 ? 360 : 2000;
    }
    public static String encodePose(int[] pose) {
        return Arrays.stream(pose).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }
    public static int[] parsePose(String text) {
        if (text == null || text.length() > 256) return null;
        String[] fields = text.split(",", -1);
        if (fields.length != STRIDE) return null;
        int[] result = new int[STRIDE];
        try {
            for (int i = 0; i < STRIDE; i++) {
                result[i] = Integer.parseInt(fields[i]);
                // Preserve legacy poses: only their old 0–2000 ms timing slots are capped.
                if(i==19 || i==20){if(result[i]<0 || result[i]>2000)return null;result[i]=Math.min(result[i],max(i));}
                else if (result[i] < min(i) || result[i] > max(i)) return null;
            }
        } catch (NumberFormatException ignored) { return null; }
        return result;
    }
    public static int[] appendDefaults(int[] original) {
        int[] result = Arrays.copyOf(original, OFFSET + SIZE);
        result[65] = 1;
        for (int i = 0; i < COUNT; i++) System.arraycopy(defaultPose(i), 0, result, OFFSET + i * STRIDE, STRIDE);
        return result;
    }
    public static int[] appendLocal(int[] original) {
        int[] result = appendDefaults(original);
        result[65] = GuardConfig.PUNCHY_COMPAT.get() ? 1 : 0;
        for (int i = 0; i < COUNT; i++) {
            int[] pose = parsePose(GuardConfig.PUNCHY_POSES.get(i).get());
            if (pose != null) {  System.arraycopy(pose, 0, result, OFFSET + i * STRIDE, STRIDE); }
        }
        return result;
    }
    public static void apply(int[] values) {
        GuardConfig.PUNCHY_RELEASE_DELAY.set(values[RELEASE_DELAY_SLOT]);
        GuardConfig.PUNCHY_COMPAT.set(values[65] != 0);
        for (int i = 0; i < COUNT; i++) GuardConfig.PUNCHY_POSES.get(i).set(encodePose(pose(values, i)));
    }
    public static int[] pose(int[] values, int index) {
        return Arrays.copyOfRange(values, OFFSET + index * STRIDE, OFFSET + (index + 1) * STRIDE);
    }
    public static double ease(double progress, int curve, int strength) {
        double t = Math.max(0, Math.min(1, progress));
        double exponent = 1 + strength / 50.0;
        return switch (curve) {
            case 1 -> Math.pow(t, exponent);
            case 2 -> 1 - Math.pow(1 - t, exponent);
            case 3 -> t < 0.5 ? Math.pow(2 * t, exponent) / 2 : 1 - Math.pow(2 * (1 - t), exponent) / 2;
            default -> t;
        };
    }
}
