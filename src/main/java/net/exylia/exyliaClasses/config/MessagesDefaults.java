package net.exylia.exyliaClasses.config;

import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSchemaRegistry;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;

@ConfigSchema(file = "messages", strict = true)
public class MessagesDefaults {

    public static void register() {
        ConfigSchemaRegistry.ensureDefaults(MessagesDefaults.class);
    }

    @ConfigValue("prefix")
    public static String PREFIX = "{primary}&lEXYLIA CLASSES &8•&r";

    @ConfigSection("warmup")
    public static class Warmup {
        @ConfigValue("timer")
        public static String TIMER = "[sound:BLOCK_NOTE_BLOCK_PLING|1.0|1.0]%prefix% {muted}Joining {highlight}%class% {muted}in {warning}%time%s{muted}...";

        @ConfigValue("ready")
        public static String READY = "[sound:ENTITY_PLAYER_LEVELUP|1.0|1.0]%prefix% {success}You are now {highlight}%class%{success}!";

        @ConfigValue("canceled")
        public static String CANCELED = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}Class change cancelled.";
    }

    @ConfigSection("class")
    public static class ClassMessages {
        @ConfigValue("removed")
        public static String REMOVED = "%prefix% {muted}You are no longer {highlight}%class%{muted}.";
    }

    @ConfigValue("no-permission")
    public static String NO_PERMISSION = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}You don't have permission to use the class {highlight}%class%{error}.";

    @ConfigSection("abilities")
    public static class Abilities {
        @ConfigValue("cooldown")
        public static String COOLDOWN = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}%ability%&r {muted}is on cooldown. {warning}%time%s {muted}remaining.";

        @ConfigValue("used")
        public static String USED = "%prefix% {success}You used {highlight}%ability%{success}!";

        @ConfigValue("clan")
        public static String CLAN = "%prefix% {muted}%player% {success}used {highlight}%ability% {muted}on your group.";

        @ConfigValue("enemy")
        public static String ENEMY = "%prefix% {error}%player% {muted}used {highlight}%ability% {error}on you!";

        @ConfigValue("no-energy")
        public static String NO_ENERGY = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}Not enough energy. Need {warning}%needed% {error}(have {warning}%current%{error}).";
    }

    @ConfigSection("weapons")
    public static class Weapons {

        @ConfigSection("mark")
        public static class Mark {
            @ConfigValue("applied")
            public static String APPLIED = "%prefix% {muted}You marked {highlight}%target%{muted}. {muted}(+{error}%damage%% {muted}dmg)";

            @ConfigValue("received")
            public static String RECEIVED = "%prefix% {muted}You were marked by {highlight}%attacker%{muted}. {muted}(+{error}%damage%% {muted}dmg)";

            @ConfigValue("expired")
            public static String EXPIRED = "%prefix% {muted}Your marks have expired.";

            @ConfigValue("same-class")
            public static String SAME_CLASS = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}You cannot mark someone of your own class.";
        }

        @ConfigSection("backstab")
        public static class Backstab {
            @ConfigValue("success")
            public static String SUCCESS = "[sound:ENTITY_PLAYER_ATTACK_CRIT|1.0|1.5]%prefix% {success}Backstab successful!";

            @ConfigValue("victim")
            public static String VICTIM = "%prefix% {error}You were backstabbed by {highlight}%attacker%{error}!";

            @ConfigValue("no-cooldown")
            public static String NO_COOLDOWN = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}Attack cooldown too low. {muted}(%current%% / %needed%%)";

            @ConfigValue("cant-dead")
            public static String CANT_DEAD = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}You cannot kill with backstab.";
        }
    }

    @ConfigSection("command")
    public static class Command {
        @ConfigValue("reload")
        public static String RELOAD = "%prefix% {success}Configuration reloaded successfully.";

        @ConfigValue("no-permission")
        public static String NO_PERMISSION_CMD = "[sound:ENTITY_VILLAGER_NO|1.0|1.0]%prefix% {error}You don't have permission to execute this command.";
    }
}
