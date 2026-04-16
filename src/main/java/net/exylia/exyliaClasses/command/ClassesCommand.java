package net.exylia.exyliaClasses.command;

import net.exylia.commons.v2.reload.api.ReloadAPI;
import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"classes", "eclasses", "exyliaclasses"})
public class ClassesCommand {

    @Subcommand("reload")
    @CommandPermission("exyliaclasses.admin.reload")
    public void reload(CommandSender sender) {
        ExyliaClasses.getInstance().getClassManager().loadClasses();
        ReloadAPI.getInstance().reloadAll(sender);
    }
}
