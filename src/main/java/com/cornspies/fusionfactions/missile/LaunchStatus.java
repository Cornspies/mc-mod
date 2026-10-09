package com.cornspies.fusionfactions.missile;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

// Whether a launcher can fire now, and if not, why (DESIGN.md §6.5 step 2). Computed by the server and sent to
// the launcher screen as two numbers: the check and its value (seconds, minutes or blocks).
public record LaunchStatus(Check check, int value) {
    public enum Check {
        READY,
        NO_MISSILE,
        NO_TARGET,
        OTHER_DIMENSION,
        TOO_CLOSE,
        TOO_FAR,
        NEEDS_SILO,
        SKY_BLOCKED,
        COOLDOWN,
        RAID_WINDOW_CLOSED,
        NO_POWER,
        SILO_INCOMPLETE,
        DISABLED;

        public String translationKey() {
            return "container.fusionfactions.missile_launcher.status." + this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static final LaunchStatus READY = new LaunchStatus(Check.READY, 0);

    public static LaunchStatus of(Check check) {
        return new LaunchStatus(check, 0);
    }

    public boolean isReady() {
        return this.check == Check.READY;
    }

    public Component message() {
        return Component.translatable(this.check.translationKey(), this.value)
                .withStyle(this.isReady() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    // Menus sync data slots as shorts.
    public int syncedValue() {
        return Mth.clamp(this.value, 0, Short.MAX_VALUE);
    }

    public static LaunchStatus fromSynced(int check, int value) {
        Check[] checks = Check.values();
        return new LaunchStatus(checks[Mth.clamp(check, 0, checks.length - 1)], value);
    }
}
