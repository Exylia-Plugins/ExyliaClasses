package net.exylia.exyliaClasses.config;

import net.exylia.lib.config.Comment;
import net.exylia.lib.config.Configs;
import net.exylia.exyliaClasses.ExyliaClasses;

/**
 * Messages for ExyliaClasses.
 *
 * <p>Generated as {@code plugins/ExyliaClasses/messages.yml} on first start.
 * Every message supports Exylia palette tokens ({@code {primary}}) and
 * placeholders ({@code %player_name%}).
 */
@Comment("ExyliaClasses messages.")
@Comment("Colour palette tokens like {primary} and placeholders like %player_name% work everywhere.")
@Comment("Messages prefixed with [sound:NAME|VOLUME|PITCH] play that sound when the message is shown.")
public record ClassesMessages(
        @Comment("Prefix before most messages.")
        String prefix,

        @Comment("Shown when a player lacks permission for a class.")
        String noPermission,

        Warmup warmup,
        ClassSection classSection,
        Abilities abilities,
        Weapons weapons,
        Command command,
        BossBars bossBars) {

    public ClassesMessages() {
        this(
            "{primary}&lEXYLIA CLASSES &8•&r",
            "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}You don't have permission to use the class {highlight}%class%{error}.",
            new Warmup(),
            new ClassSection(),
            new Abilities(),
            new Weapons(),
            new Command(),
            new BossBars()
        );
    }

    @Comment("The boss bar titles; their colour and segments are in config.yml.")
    public record BossBars(
            @Comment("The energy bar. %energy_color%, %symbol%, %energy% and %max% come from the class.")
            String energy,

            @Comment("The mark bar. %mark_color%, %mark_icon%, %extra_damage% and %time% come from the mark.")
            String mark
    ) {
        public BossBars() {
            this(
                "%energy_color%%symbol% %energy% / %max%",
                "%mark_color%%mark_icon% +%extra_damage%% {muted}(%time%)"
            );
        }
    }

    @Comment("Warmup messages, shown while waiting to join a class.")
    public record Warmup(
            @Comment("Sent repeatedly during the warmup countdown.")
            String timer,

            @Comment("Sent when the warmup completes.")
            String ready,

            @Comment("Sent when the warmup is cancelled.")
            String canceled
    ) {
        public Warmup() {
            this(
                "[sound:BLOCK_NOTE_BLOCK_PLING|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {muted}Joining {highlight}%class% {muted}in {warning}%duration% ⌚{muted}...",
                "[sound:ENTITY_PLAYER_LEVELUP|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {success}You are now {highlight}%class%{success}!",
                "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}Class change cancelled."
            );
        }
    }

    @Comment("Messages about classes in general.")
    public record ClassSection(
            @Comment("Sent when the player leaves their class.")
            String removed
    ) {
        public ClassSection() {
            this("{primary}&lEXYLIA CLASSES &8•&r {muted}You are no longer {highlight}%class%{muted}.");
        }
    }

    @Comment("Ability-related messages.")
    public record Abilities(
            @Comment("Shown when an ability is on cooldown.")
            String cooldown,

            @Comment("Shown when an ability is used successfully.")
            String used,

            @Comment("Broadcast to clan members when an ally uses an ability.")
            String clan,

            @Comment("Shown to enemies hit by an ability.")
            String enemy,

            @Comment("Shown when there isn't enough energy.")
            String noEnergy
    ) {
        public Abilities() {
            this(
                "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}%ability%&r {muted}is on cooldown. {warning}%duration% ⌚ {muted}remaining.",
                "{primary}&lEXYLIA CLASSES &8•&r {success}You used {highlight}%ability%{success}!",
                "{primary}&lEXYLIA CLASSES &8•&r {muted}%player% {success}used {highlight}%ability% {muted}on your group.",
                "{primary}&lEXYLIA CLASSES &8•&r {error}%player% {muted}used {highlight}%ability% {error}on you!",
                "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}Not enough energy. Need {warning}%needed% {error}(have {warning}%current%{error})."
            );
        }
    }

    @Comment("Weapon-related messages.")
    public record Weapons(
            Mark mark,
            Backstab backstab
    ) {
        public Weapons() { this(new Mark(), new Backstab()); }

        @Comment("Mark weapon messages.")
        public record Mark(
                @Comment("Shown when you mark someone.")
                String applied,

                @Comment("Shown when you are marked.")
                String received,

                @Comment("Shown when your marks expire.")
                String expired,

                @Comment("Shown when trying to mark someone of the same class.")
                String sameClass
        ) {
            public Mark() {
                this(
                    "{primary}&lEXYLIA CLASSES &8•&r {muted}You marked {highlight}%target%{muted}. {muted}(+{error}%damage%% {muted}dmg)",
                    "{primary}&lEXYLIA CLASSES &8•&r {muted}You were marked by {highlight}%attacker%{muted}. {muted}(+{error}%damage%% {muted}dmg)",
                    "{primary}&lEXYLIA CLASSES &8•&r {muted}Your marks have expired.",
                    "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}You cannot mark someone of your own class."
                );
            }
        }

        @Comment("Backstab weapon messages.")
        public record Backstab(
                @Comment("Shown on a successful backstab.")
                String success,

                @Comment("Shown to the victim of a backstab.")
                String victim,

                @Comment("Shown when the attack cooldown is too low.")
                String noCooldown,

                @Comment("Shown when the backstab would kill the target.")
                String cantDead
        ) {
            public Backstab() {
                this(
                    "[sound:ENTITY_PLAYER_ATTACK_CRIT|1.0|1.5]{primary}&lEXYLIA CLASSES &8•&r {success}Backstab successful!",
                    "{primary}&lEXYLIA CLASSES &8•&r {error}You were backstabbed by {highlight}%attacker%{error}!",
                    "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}Attack cooldown too low. {muted}(%current%% / %needed%%)",
                    "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}You cannot kill with backstab."
                );
            }
        }
    }

    @Comment("Command messages.")
    public record Command(
            @Comment("Shown after /classes reload.")
            String reload,

            @Comment("Shown when a player lacks permission for a command.")
            String noPermission
    ) {
        public Command() {
            this(
                "{primary}&lEXYLIA CLASSES &8•&r {success}Configuration reloaded successfully.",
                "[sound:ENTITY_VILLAGER_NO|1.0|1.0]{primary}&lEXYLIA CLASSES &8•&r {error}You don't have permission to execute this command."
            );
        }
    }

    /** Loads and returns the messages, creating the file on first access. */
    public static ClassesMessages load(org.bukkit.plugin.Plugin plugin) {
        return Configs.define(plugin, "messages", ClassesMessages.class).translated().load().get();
    }

    /** The same, for callers that hold the plugin rather than the Bukkit one. */
    public static ClassesMessages load(ExyliaClasses plugin) {
        return load(plugin.getPlugin());
    }
}
