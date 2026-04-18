package net.exylia.exyliaClasses;

import lombok.Getter;
import net.exylia.commons.v2.clan.api.ClanAPI;
import net.exylia.commons.v2.clientapi.api.ClientAPI;
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

public class ExyliaClasses extends ExyliaLoaderPlugin {

    @Getter
    private static ExyliaClasses instance;

    @Getter
    private ClassManager classManager;

    @Getter
    private ClassesAPI classesAPI;

    private HoldEffectListener holdEffectListener;
    private SpecialPassiveListener specialPassiveListener;

    @Override
    protected void onPreExyliaEnable() {
        instance = this;
        CommandRegistry.register(this);
    }

    @Override
    protected void onExyliaEnable() {
        DebugAPI.init(getPlugin());
        loadConfigs();
        ClanAPI.initialize(getPlugin());
        ClientAPI.initialize(getPlugin());

        PlayerStateManager stateManager = new PlayerStateManager();
        CooldownManager cooldownManager = new CooldownManager();
        classManager = new ClassManager(getPlugin(), stateManager, cooldownManager);
        classManager.loadClasses();

        classesAPI = new ClassesAPI(classManager);

        loadListeners();
        loadPlaceholders();
        checkOnlinePlayers();
    }

    private void checkOnlinePlayers() {
        for (var player : getPlugin().getServer().getOnlinePlayers()) {
            classManager.checkArmorChange(player);
        }
    }

    @Override
    protected void onExyliaDisable() {
        if (holdEffectListener != null) holdEffectListener.stop();
        if (specialPassiveListener != null) specialPassiveListener.stop();
        if (classManager != null) classManager.disableAll();
        Placeholders.shutdown();
    }

    private void loadConfigs() {
        GlobalDefaults.register();
        MessagesDefaults.register();
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
        Placeholders.initialize(getPlugin());
        Placeholders.registerAnnotatedClass(new ClassesPlaceholders(classesAPI));
        Placeholders.registerPapiExpander("classes");
    }
}
