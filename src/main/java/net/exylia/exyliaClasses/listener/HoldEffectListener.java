package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.HoldEffect;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HoldEffectListener {

    private final ClassManager classManager;
    private ScheduledTask task;

    public HoldEffectListener(ClassManager classManager) {
        this.classManager = classManager;
    }

    public void start() {
        task = TaskAPI.syncTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                classManager.getStateManager().get(player).ifPresent(state -> {
                    if (!state.isInClass()) return;
                    ClassDefinition classDef = state.getCurrentClass();

                    Map<Material, HoldEffect> holdEffects = classDef.getHoldEffects();
                    if (holdEffects.isEmpty()) return;

                    Material heldMaterial = player.getInventory().getItemInMainHand().getType();
                    HoldEffect holdEffect = holdEffects.get(heldMaterial);
                    if (holdEffect == null) return;

                    List<Player> targets = ClanHelper.getTargets(player, holdEffect.getType(), holdEffect.getRadius());
                    for (Player target : targets) {
                        EffectAPI.applyAll(target, holdEffect.getEffects());
                    }
                });
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
