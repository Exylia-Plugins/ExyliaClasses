package net.exylia.exyliaClasses;

import com.lukittu.loader.LoaderPlugin;
import com.lukittu.loader.spigot.SpigotLukittuLoader;
import lombok.Getter;
import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.command.CommandRegistry;
import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.listener.*;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.PlayerStateManager;
import net.exylia.exyliaClasses.placeholder.ClassesPlaceholders;
import net.exylia.lib.config.Configs;
import net.exylia.lib.reload.Reloads;
import net.exylia.lib.text.Prefixes;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * The plugin itself.
 *
 * <p>This is not a {@link JavaPlugin}: the Lukittu loader is the plugin Bukkit
 * knows about, and it hands this class the {@code JavaPlugin} to register
 * against. Extending {@code JavaPlugin} here makes the loader's
 * {@code asSubclass(LoaderPlugin.class)} fail with a {@code ClassCastException}
 * the moment it tries to start the inner plugin.
 */
public final class ExyliaClasses implements LoaderPlugin<SpigotLukittuLoader> {

    @Getter
    private static ExyliaClasses instance;

    @Getter
    private ClassManager classManager;

    @Getter
    private ClassesAPI classesAPI;

    /** The Bukkit plugin everything registers against — the loader. */
    @Getter
    private JavaPlugin plugin;

    private SpigotLukittuLoader loader;
    private HoldEffectListener holdEffectListener;
    private SpecialPassiveListener specialPassiveListener;
    private ClassesConfig config;
    private Reloads reloads;
    private Logger log;

    @Override
    public void start(SpigotLukittuLoader lukittuLoader) {
        instance = this;
        this.loader = lukittuLoader;
        this.plugin = lukittuLoader.getPlugin();
        this.log = plugin.getLogger();

        CommandRegistry.register(this);

        loadConfig();
        log.info("ExyliaLib: Clans=" + net.exylia.lib.clan.Clans.isSupported()
            + " Clients=" + net.exylia.lib.client.Clients.isSupported());

        PlayerStateManager stateManager = new PlayerStateManager();
        classManager = new ClassManager(plugin, stateManager);
        classManager.loadClasses();

        classesAPI = new ClassesAPI(classManager);
        reloads = Reloads.of(plugin)
                .step("configs", () -> Configs.reloadAll(plugin))
                .step("prefix", () -> Prefixes.set(plugin, ClassesMessages.load(plugin).prefix()))
                .step("classes", classManager::loadClasses)
                .step("active players", this::refreshOnlinePlayers);

        // The reload only runs when asked; enable has to set the prefix itself.
        Prefixes.set(plugin, ClassesMessages.load(plugin).prefix());

        loadListeners();
        loadPlaceholders();
        checkOnlinePlayers();
    }

    @Override
    public void shutdown(SpigotLukittuLoader lukittuLoader) {
        if (holdEffectListener != null) holdEffectListener.stop();
        if (specialPassiveListener != null) specialPassiveListener.stop();
        if (classManager != null) classManager.disableAll();
    }

    private void loadConfig() {
        config = ClassesConfig.load(this);
    }

    private void loadListeners() {
        new ArmorListener(classManager);
        new AbilityListener(classManager);
        new MarkListener(classManager);
        new BackstabListener(classManager);

        holdEffectListener = new HoldEffectListener(classManager);
        holdEffectListener.start();

        specialPassiveListener = new SpecialPassiveListener(classManager);
        specialPassiveListener.start();
    }

    private void loadPlaceholders() {
        new ClassesPlaceholders(classesAPI).register(plugin);
    }

    private void checkOnlinePlayers() {
        for (var player : plugin.getServer().getOnlinePlayers()) {
            classManager.checkArmorChange(player);
        }
    }

    private void refreshOnlinePlayers() {
        for (var player : plugin.getServer().getOnlinePlayers()) {
            classManager.removeAll(player);
            classManager.checkArmorChange(player);
        }
    }

    public void reload(CommandSender sender) {
        reloads.run(sender);
    }

    public Logger log() { return log; }
}
