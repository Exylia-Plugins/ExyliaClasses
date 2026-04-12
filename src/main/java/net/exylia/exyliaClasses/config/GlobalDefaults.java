package net.exylia.exyliaClasses.config;

import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;
import net.exylia.commons.v2.visual.config.BossBarConfig;
import net.kyori.adventure.bossbar.BossBar;

@ConfigSchema(file = "config")
public class GlobalDefaults {

    public static void register() {
        ConfigSchemaRegistry.ensureDefaults(GlobalDefaults.class);
    }

    @ConfigSection("bossbars")
    public static class BossBars {

        @ConfigSection("energy")
        public static class Energy {
            @ConfigValue("enabled")
            public static boolean ENABLED = true;

            @ConfigValue("config")
            public static BossBarConfig CONFIG = BossBarConfig.builder("%energy_color%%symbol% %energy% / %max%")
                    .color(BossBar.Color.YELLOW)
                    .style(BossBar.Overlay.NOTCHED_20)
                    .updateInterval(1L)
                    .build();
        }

        @ConfigSection("mark")
        public static class Mark {
            @ConfigValue("enabled")
            public static boolean ENABLED = true;

            @ConfigValue("config")
            public static BossBarConfig CONFIG = BossBarConfig.builder("%mark_color%%mark_icon% +%extra_damage%% {muted}(%time_formatted%)")
                    .color(BossBar.Color.RED)
                    .style(BossBar.Overlay.NOTCHED_20)
                    .build();
        }
    }
}
