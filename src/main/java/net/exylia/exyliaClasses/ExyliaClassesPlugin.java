package net.exylia.exyliaClasses;

import net.exylia.exyliaClasses.bootstrap.ExyliaLibInstaller;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Bukkit entry point, kept free of ExyliaLib types so it still runs when ExyliaLib is missing.
 *
 * <p>Without ExyliaLib it installs the library and disables itself until the restart; with it, it
 * hands over to {@link ExyliaClasses}.
 */
public final class ExyliaClassesPlugin extends JavaPlugin {

    private ExyliaClasses core;

    @Override
    public void onEnable() {
        Plugin lib = getServer().getPluginManager().getPlugin("ExyliaLib");
        if (lib == null || !lib.isEnabled()) {
            ExyliaLibInstaller.install(this);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        core = new ExyliaClasses();
        core.start(this);
    }

    @Override
    public void onDisable() {
        if (core != null) core.shutdown();
        core = null;
    }
}
