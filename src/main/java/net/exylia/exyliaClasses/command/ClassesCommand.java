package net.exylia.exyliaClasses.command;

import net.exylia.exyliaClasses.ExyliaClasses;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/** Staff commands, under {@code /classesadmin}; the old {@code /classes <command>} spellings keep working. */
public class ClassesCommand {

    @Command({"classesadmin reload", "classes reload", "eclasses reload", "exyliaclasses reload"})
    @CommandPermission("exyliaclasses.admin.reload")
    public void reload(CommandSender sender) {
        ExyliaClasses.getInstance().reload(sender);
    }

    @Command({"classesadmin settings", "classes settings", "eclasses settings", "exyliaclasses settings"})
    @CommandPermission("exyliaclasses.admin.settings")
    public void settings(Player player) {
        ExyliaClasses.getInstance().openSettings(player);
    }
}
