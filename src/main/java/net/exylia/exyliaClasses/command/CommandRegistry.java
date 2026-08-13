package net.exylia.exyliaClasses.command;

import net.exylia.exyliaClasses.ExyliaClasses;
import revxrsal.commands.bukkit.BukkitLamp;

public final class CommandRegistry {

    private CommandRegistry() {}

    public static void register(ExyliaClasses plugin) {
        var lamp = BukkitLamp.builder(plugin.getPlugin()).build();
        lamp.register(new ClassesCommand());
    }
}
