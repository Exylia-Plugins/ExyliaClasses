package net.exylia.exyliaClasses;

import lombok.Getter;
import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.api.ClassesServiceImpl;
import net.exylia.exyliaClasses.command.CommandRegistry;
import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.listener.*;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.PlayerStateManager;
import net.exylia.exyliaClasses.placeholder.ClassesPlaceholders;
import net.exylia.lib.config.Configs;
import net.exylia.lib.debug.Debug;
import net.exylia.lib.reload.Reloads;
import net.exylia.lib.text.Prefixes;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * The plugin itself, started by {@link ExyliaClassesPlugin} once ExyliaLib is present.
 */
public final class ExyliaClasses {

    @Getter
    private static ExyliaClasses instance;

    @Getter
    private ClassManager classManager;

    @Getter
    private ClassesAPI classesAPI;

    /** The Bukkit plugin everything registers against. */
    @Getter
    private JavaPlugin plugin;

    private HoldEffectListener holdEffectListener;
    private SpecialPassiveListener specialPassiveListener;
    private ClassesConfig config;
    private Reloads reloads;
    private Debug debug;

    public void start(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;

        CommandRegistry.register(this);

        loadConfig();
        debug = Debug.of(plugin).enabled(config.debug());
        debug.motd();
        debug.log("ExyliaLib: Clans=" + net.exylia.lib.clan.Clans.isSupported()
            + " Clients=" + net.exylia.lib.client.Clients.isSupported());

        PlayerStateManager stateManager = new PlayerStateManager();
        classManager = new ClassManager(plugin, stateManager);
        classManager.loadClasses();

        classesAPI = new ClassesAPI(classManager);
        // The public API, for plugins outside the suite. Registered here rather
        // than at the end of start(): the class manager it reads is up, and a
        // plugin enabling after us must find it whatever order the server chose.
        ClassesServiceImpl.register(plugin);
        reloads = Reloads.of(plugin)
                .step("configs", () -> Configs.reloadAll(plugin))
                .step("prefix", () -> Prefixes.set(plugin, ClassesMessages.load(plugin).prefix()))
                .step("debug flag", () -> debug.enabled(ClassesConfig.load(plugin).debug()))
                .step("classes", classManager::loadClasses)
                .step("active players", this::refreshOnlinePlayers);

        // The reload only runs when asked; enable has to set the prefix itself.
        Prefixes.set(plugin, ClassesMessages.load(plugin).prefix());

        loadListeners();
        loadPlaceholders();
        checkOnlinePlayers();
    }

    public void shutdown() {
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

    public Debug debug() { return debug; }
}
