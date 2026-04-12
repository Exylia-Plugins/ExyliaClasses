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
    public static String PREFIX = "<#8a51c4>&lClases &8» &r";

    @ConfigSection("warmup")
    public static class Warmup {
        @ConfigValue("timer")
        public static String TIMER = "%prefix%&7Entrando a &f%class% &7en &e%time%s&7...";

        @ConfigValue("ready")
        public static String READY = "%prefix%&a¡Ahora eres &f%class%&a!";

        @ConfigValue("canceled")
        public static String CANCELED = "%prefix%&cCambio de clase cancelado.";
    }

    @ConfigSection("class")
    public static class ClassMessages {
        @ConfigValue("removed")
        public static String REMOVED = "%prefix%&7Ya no eres &f%class%&7.";
    }

    @ConfigValue("no-permission")
    public static String NO_PERMISSION = "%prefix%&cNo tienes permiso para usar la clase &f%class%&c.";

    @ConfigSection("abilities")
    public static class Abilities {
        @ConfigValue("cooldown")
        public static String COOLDOWN = "%prefix%&c%ability% &7está en cooldown. &e%time%s &7restantes.";

        @ConfigValue("used")
        public static String USED = "%prefix%&aUsaste &f%ability%&a!";

        @ConfigValue("clan")
        public static String CLAN = "%prefix%&7%player% &ausó &f%ability%&7 en tu grupo.";

        @ConfigValue("enemy")
        public static String ENEMY = "%prefix%&c%player% &7usó &f%ability% &cen ti!";

        @ConfigValue("no-energy")
        public static String NO_ENERGY = "%prefix%&cNo tienes suficiente energía. Necesitas &e%needed% &c(tienes &e%current%&c).";
    }

    @ConfigSection("weapons")
    public static class Weapons {

        @ConfigSection("mark")
        public static class Mark {
            @ConfigValue("applied")
            public static String APPLIED = "%prefix%&7Marcaste a &f%target%&7. &8(+&c%damage%% &8daño)";

            @ConfigValue("received")
            public static String RECEIVED = "%prefix%&7Fuiste marcado por &f%attacker%&7. &8(+&c%damage%% &8daño)";

            @ConfigValue("expired")
            public static String EXPIRED = "%prefix%&7Tus marcas han expirado.";

            @ConfigValue("same-class")
            public static String SAME_CLASS = "%prefix%&cNo puedes marcar a alguien de tu misma clase.";
        }

        @ConfigSection("backstab")
        public static class Backstab {
            @ConfigValue("success")
            public static String SUCCESS = "%prefix%&a¡Backstab exitoso!";

            @ConfigValue("victim")
            public static String VICTIM = "%prefix%&c¡Recibiste un backstab de &f%attacker%&c!";

            @ConfigValue("no-cooldown")
            public static String NO_COOLDOWN = "%prefix%&cNecesitas más cooldown de ataque &8(%current%&8% / &7%needed%&8%).";

            @ConfigValue("cant-dead")
            public static String CANT_DEAD = "%prefix%&cNo puedes matar con el backstab.";
        }
    }

    @ConfigSection("command")
    public static class Command {
        @ConfigValue("reload")
        public static String RELOAD = "%prefix%&aConfiguración recargada correctamente.";

        @ConfigValue("no-permission")
        public static String NO_PERMISSION_CMD = "%prefix%&cNo tienes permiso para ejecutar este comando.";
    }
}
