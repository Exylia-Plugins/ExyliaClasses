package net.exylia.exyliaClasses;

import lombok.Getter;
import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.command.CommandRegistry;
import net.exylia.exyliaClasses.config.GlobalDefaults;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.exyliaClasses.listener.*;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.CooldownManager;
import net.exylia.exyliaClasses.manager.PlayerStateManager;
import net.exylia.exyliaClasses.placeholder.ClassesPlaceholders;
import net.exylia.commons.v2.loader.ExyliaLoaderPlugin;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import org.bukkit.Server;

public class ExyliaClasses extends ExyliaLoaderPlugin {

    @Getter
    private static ExyliaClasses instance;

    @Getter
    private ClassManager classManager;

    @Getter
    private ClassesAPI classesAPI;

    private HoldEffectListener holdEffectListener;

    @Override
    protected void onPreExyliaEnable() {
        instance = this;
        CommandRegistry.register(this);
    }

    @Override
    protected void onExyliaEnable() {
        DebugAPI.init(getPlugin());
        loadConfigs();

        PlayerStateManager stateManager = new PlayerStateManager();
        CooldownManager cooldownManager = new CooldownManager();
        classManager = new ClassManager(getPlugin(), stateManager, cooldownManager);
        classManager.loadClasses();

        classesAPI = new ClassesAPI(classManager);

        loadListeners();
        loadPlaceholders();

        DebugAPI.logPluginSuccess("ExyliaClasses habilitado con "
                + classManager.getClasses().size() + " clases.");
    }

    @Override
    protected void onExyliaDisable() {
        if (holdEffectListener != null) holdEffectListener.stop();
        if (classManager != null) classManager.disableAll();
        Placeholders.shutdown();
    }

    private void loadConfigs() {
        GlobalDefaults.register();
        MessagesDefaults.register();
        DebugAPI.logPluginSuccess("Configuración cargada");
    }

    private void loadListeners() {
        new ArmorListener(classManager);
        new AbilityListener(classManager);
        new MarkListener(classManager);
        new BackstabListener(classManager);

        holdEffectListener = new HoldEffectListener(classManager);
        holdEffectListener.start();

        DebugAPI.logPluginSuccess("Listeners registrados");
    }

    private void loadPlaceholders() {
        Placeholders.initialize(getPlugin());
        Placeholders.registerAnnotatedClass(new ClassesPlaceholders(classesAPI));
        Placeholders.registerPapiExpander("classes");
        DebugAPI.logPluginSuccess("Placeholders registrados");
    }
}
