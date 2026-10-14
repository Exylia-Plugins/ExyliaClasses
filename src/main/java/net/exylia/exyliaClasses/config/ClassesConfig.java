package net.exylia.exyliaClasses.config;

import net.exylia.lib.config.Comment;
import net.exylia.lib.config.Configs;
import net.exylia.lib.config.Languages;
import net.exylia.lib.config.Migration;
import net.exylia.exyliaClasses.ExyliaClasses;

import java.util.Map;

/**
 * Main configuration for ExyliaClasses.
 *
 * <p>Generated as {@code plugins/ExyliaClasses/config.yml} on first start.
 */
@Comment("ExyliaClasses configuration.")
public record ClassesConfig(
        @Comment("Language of this plugin's menus and messages. 'default' follows the language set in")
        @Comment("ExyliaLib's config.yml; en, es, pt, fr, ru or tr set this plugin alone. Each one is a folder under")
        @Comment("lang/ you can edit; 'custom' holds the files this server had before languages existed.")
        String language,

        @Comment("Print diagnostic lines to the console: integration status, load detail.")
        @Comment("Off in production; turn it on when something behaves oddly.")
        boolean debug,

        @Comment("Energy bar displayed above the hotbar.")
        @Comment("")
        BossBarConfig energyBar,

        @Comment("Mark duration bar.")
        BossBarConfig markBar) {

    public ClassesConfig() {
        this(Languages.DEFAULT, false, BossBarConfig.energy(), BossBarConfig.mark());
    }

    /** Version 1 to 2: a server that ran the plugin before languages keeps its files, in {@code lang/custom/}. */
    public static final Migration MIGRATION_FROM_1 = Languages.ADOPT_EXISTING;

    /** Version 2 to 3: the boss bar titles move into {@code messages.yml}, where they are translated. */
    public static final Map<String, String> MOVED_TO_MESSAGES = Map.of(
            "energy-bar.text", "boss-bars.energy",
            "mark-bar.text", "boss-bars.mark");

    /** Boss bar settings. */
    @Comment("Boss bar configuration.")
    @Comment("The bar titles are in messages.yml.")
    @Comment("colour: PINK, BLUE, RED, GREEN, YELLOW, PURPLE or WHITE.")
    @Comment("overlay: PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12 or NOTCHED_20.")
    public record BossBarConfig(
            @Comment("Whether to show this bar.")
            boolean enabled,

            @Comment("Bar colour. PINK, BLUE, RED, GREEN, YELLOW, PURPLE or WHITE.")
            String colour,

            @Comment("Bar segmentation. PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12 or NOTCHED_20.")
            String overlay
    ) {
        public BossBarConfig() {
            this(true, "PURPLE", "PROGRESS");
        }

        /**
         * The energy bar as it ships.
         *
         * <p>Notched into twenty segments, so a glance at the bar reads as a
         * number of pips rather than a length to estimate.
         */
        static BossBarConfig energy() {
            return new BossBarConfig(true, "YELLOW", "NOTCHED_20");
        }

        /**
         * The mark bar as it ships.
         *
         * <p>Continuous rather than notched: it tracks time running out, which
         * is a smooth thing, and {@code %time%} spells out the same number.
         */
        static BossBarConfig mark() {
            return new BossBarConfig(true, "RED", "PROGRESS");
        }
    }

    /** Loads and returns the config, creating it on first access. */
    public static ClassesConfig load(org.bukkit.plugin.Plugin plugin) {
        return Configs.define(plugin, "config", ClassesConfig.class)
                .version(3)
                .migration(1, MIGRATION_FROM_1)
                .migration(2, Configs.moveTo(plugin, "messages", MOVED_TO_MESSAGES))
                .load().get();
    }

    /** The same, for callers that hold the plugin rather than the Bukkit one. */
    public static ClassesConfig load(ExyliaClasses plugin) {
        return load(plugin.getPlugin());
    }
}
