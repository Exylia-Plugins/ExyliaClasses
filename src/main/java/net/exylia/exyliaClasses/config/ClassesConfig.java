package net.exylia.exyliaClasses.config;

import net.exylia.lib.config.Comment;
import net.exylia.lib.config.Configs;
import net.exylia.exyliaClasses.ExyliaClasses;

/**
 * Main configuration for ExyliaClasses.
 *
 * <p>Generated as {@code plugins/ExyliaClasses/config.yml} on first start.
 */
@Comment("ExyliaClasses configuration.")
public record ClassesConfig(
        @Comment("Print diagnostic lines to the console: integration status, load detail.")
        @Comment("Off in production; turn it on when something behaves oddly.")
        boolean debug,

        @Comment("Energy bar displayed above the hotbar.")
        @Comment("")
        BossBarConfig energyBar,

        @Comment("Mark duration bar.")
        BossBarConfig markBar) {

    public ClassesConfig() {
        this(false, BossBarConfig.energy(), BossBarConfig.mark());
    }

    /** Boss bar settings. */
    @Comment("Boss bar configuration.")
    @Comment("text: the bar title with placeholders.")
    @Comment("colour: PINK, BLUE, RED, GREEN, YELLOW, PURPLE or WHITE.")
    @Comment("overlay: PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12 or NOTCHED_20.")
    public record BossBarConfig(
            @Comment("Whether to show this bar.")
            boolean enabled,

            @Comment("The bar title. Supports Exylia palette tokens and placeholders.")
            String text,

            @Comment("Bar colour. PINK, BLUE, RED, GREEN, YELLOW, PURPLE or WHITE.")
            String colour,

            @Comment("Bar segmentation. PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12 or NOTCHED_20.")
            String overlay
    ) {
        public BossBarConfig() {
            this(true, "", "PURPLE", "PROGRESS");
        }

        /**
         * The energy bar as it ships.
         *
         * <p>Notched into twenty segments, so a glance at the bar reads as a
         * number of pips rather than a length to estimate.
         */
        static BossBarConfig energy() {
            return new BossBarConfig(true,
                    "%energy_color%%symbol% %energy% / %max%",
                    "YELLOW", "NOTCHED_20");
        }

        /**
         * The mark bar as it ships.
         *
         * <p>Continuous rather than notched: it tracks time running out, which
         * is a smooth thing, and {@code %time%} spells out the same number.
         */
        static BossBarConfig mark() {
            return new BossBarConfig(true,
                    "%mark_color%%mark_icon% +%extra_damage%% {muted}(%time%)",
                    "RED", "PROGRESS");
        }
    }

    /** Loads and returns the config, creating it on first access. */
    public static ClassesConfig load(org.bukkit.plugin.Plugin plugin) {
        return Configs.define(plugin, "config", ClassesConfig.class).load().get();
    }

    /** The same, for callers that hold the plugin rather than the Bukkit one. */
    public static ClassesConfig load(ExyliaClasses plugin) {
        return load(plugin.getPlugin());
    }
}
