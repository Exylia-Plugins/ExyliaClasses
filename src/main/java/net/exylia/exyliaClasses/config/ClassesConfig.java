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
        @Comment("Energy bar displayed above the hotbar.")
        @Comment("")
        BossBarConfig energyBar,

        @Comment("Mark duration bar.")
        BossBarConfig markBar) {

    public ClassesConfig() {
        this(new BossBarConfig(), new BossBarConfig());
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
    }

    /** Loads and returns the config, creating it on first access. */
    public static ClassesConfig load(ExyliaClasses plugin) {
        return Configs.define(plugin, "config", ClassesConfig.class).load().get();
    }
}
