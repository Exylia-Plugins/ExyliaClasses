package net.exylia.exyliaClasses.command;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("eclasses")
public class ClassesCommand {

    @Subcommand("reload")
    @CommandPermission("exyliaclasses.admin.reload")
    public void reload(CommandSender sender) {
        ExyliaClasses.getInstance().getClassManager().loadClasses();
        MessageAPI.send(sender, Messages.get("command.reload"));
    }
}
