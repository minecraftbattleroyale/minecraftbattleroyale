package io.github.minecraftbattleroyale;

import com.google.common.base.Preconditions;
import org.spongepowered.api.text.Text;

public final class MathUtil {
    private static final int TICKS = 20;

    /** Converts ticks to secs. */
    public static int sec(int ticks) {
        Preconditions.checkArgument(ticks >= 0);
        return ticks / TICKS;
    }

    /** Convert secs to ticks */
    public static int ticks(int sec) {
        Preconditions.checkArgument(sec >= 0);
        return sec * TICKS;
    }

    /** Convert integer to a float */
    public static float percent(int total, int position) {
        return (float) ((double) position / (double) total) * 100;
    }

    public static Text countTitle(String time, float percent) {
        int percentInt = (int) (((percent * 10) / 10) * .80);
        String bar = textLine(time, 80, '.', "&d", "");
        String footer;

        // If percent is bigger than no no zone print normal
        if (percentInt > 43 + time.length()) {
            footer = bar.substring(0, percentInt) + "&5" + bar.substring(percentInt);
        }
        // Don't show position in clock
        else if (percentInt > 40 && percentInt < 44 + time.length()) {
            footer = bar.substring(0, 43 + time.length()) + "&5" + bar.substring(43 + time.length());
        }
        else {
            footer = bar.substring(0, 43 + time.length()) + "&5" + bar.substring(43 + time.length());
            footer = footer.substring(0, percentInt <= 1 ? 2 : percentInt) + "&5" + footer.substring(percentInt <= 1 ? 2 : percentInt);
        }

        return Text.of(footer);
    }

    public static String textLine(String text, int size, char delimiter, String lineColor, String textColor) {
        int padding = Math.abs(MessageUtil.stripColors(text).length() - size) / 2;
        StringBuilder side = new StringBuilder();

        for (int i = 0; i < padding; i++) {
            side.append(delimiter);
        }

        return String.format("%s%s%s %s %s%s", lineColor, side, textColor, text, lineColor, side);
    }
}