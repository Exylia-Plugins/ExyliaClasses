package net.exylia.exyliaClasses.command;

import net.exylia.exyliaClasses.ExyliaClasses;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"classes", "eclasses", "exyliaclasses"})
public class ClassesCommand {

    @Subcommand("reload")
    @CommandPermission("exyliaclasses.admin.reload")
    public void reload(CommandSender sender) {
        ExyliaClasses.getInstance().reload(sender);
    }

    @Subcommand("settings")
    @CommandPermission("exyliaclasses.admin.settings")
    public void settings(Player player) {
        ExyliaClasses.getInstance().openSettings(player);
    }
}
