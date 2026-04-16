package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.HoldEffect;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HoldEffectListener {

    private final ClassManager classManager;
    private ScheduledTask task;

    public HoldEffectListener(ClassManager classManager) {
        this.classManager = classManager;
    }

    public void start() {
        task = TaskAPI.syncTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (classManager.getStateManager().get(player)
                        .filter(s -> s.isInClass() && !s.getCurrentClass().getHoldEffects().isEmpty())
                        .isEmpty()) continue;

                TaskAPI.atLater(player, () -> {
                    classManager.getStateManager().get(player).ifPresent(state -> {
                        if (!state.isInClass()) return;
                        ClassDefinition classDef = state.getCurrentClass();

                        Map<Material, HoldEffect> holdEffects = classDef.getHoldEffects();
                        if (holdEffects.isEmpty()) return;

                        Material heldMaterial = player.getInventory().getItemInMainHand().getType();
                        HoldEffect holdEffect = holdEffects.get(heldMaterial);
                        if (holdEffect == null) return;

                        DebugAPI.logPluginDebug("[HoldEffect] " + player.getName() + " holding " + heldMaterial + " | type=" + holdEffect.getType() + " radius=" + holdEffect.getRadius() + " effects=" + holdEffect.getEffects());
                        List<Player> targets = ClanHelper.getTargets(player, holdEffect.getType(), holdEffect.getRadius());
                        DebugAPI.logPluginDebug("[HoldEffect] applying to " + targets.stream().map(Player::getName).toList());

                        List<String> effects = holdEffect.getEffects();
                        for (Player target : targets) {
                            TaskAPI.atLater(target, () -> EffectAPI.applyAll(target, effects), 0L, TimeUnit.MILLISECONDS);
                        }
                    });
                }, 0L, TimeUnit.MILLISECONDS);
            }
        }, 0L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
