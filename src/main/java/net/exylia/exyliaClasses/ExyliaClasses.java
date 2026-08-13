package net.exylia.exyliaClasses;

import lombok.Getter;
import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.command.CommandRegistry;
import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.exyliaClasses.listener.*;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.PlayerStateManager;
import net.exylia.exyliaClasses.placeholder.ClassesPlaceholders;
import net.exylia.lib.config.Configs;
import net.exylia.lib.reload.Reloads;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class ExyliaClasses extends JavaPlugin {

    @Getter
    private static ExyliaClasses instance;

    @Getter
    private ClassManager classManager;

    @Getter
    private ClassesAPI classesAPI;

    private HoldEffectListener holdEffectListener;
    private SpecialPassiveListener specialPassiveListener;
    private ClassesConfig config;
    private Reloads reloads;
    private Logger log;

    @Override
    public void onEnable() {
        instance = this;
        this.log = getLogger();

        CommandRegistry.register(this);

        loadConfig();
        log.info("ExyliaLib: Clans=" + net.exylia.lib.clan.Clans.isSupported()
            + " Clients=" + net.exylia.lib.client.Clients.isSupported());

        PlayerStateManager stateManager = new PlayerStateManager();
        classManager = new ClassManager(this, stateManager);
        classManager.loadClasses();

        classesAPI = new ClassesAPI(classManager);
        reloads = Reloads.of(this)
                .step("configs", () -> Configs.reloadAll(this))
                .step("classes", classManager::loadClasses)
                .step("active players", this::refreshOnlinePlayers);

        loadListeners();
        loadPlaceholders();
        checkOnlinePlayers();
    }

    @Override
    public void onDisable() {
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
        new ClassesPlaceholders(classesAPI).register(this);
    }

    private void checkOnlinePlayers() {
        for (var player : getServer().getOnlinePlayers()) {
            classManager.checkArmorChange(player);
        }
    }

    private void refreshOnlinePlayers() {
        for (var player : getServer().getOnlinePlayers()) {
            classManager.removeAll(player);
            classManager.checkArmorChange(player);
        }
    }

    public void reload(CommandSender sender) {
        reloads.run(sender);
    }

    public Logger log() { return log; }
}
