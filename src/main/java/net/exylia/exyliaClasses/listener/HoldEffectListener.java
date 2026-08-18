package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.HoldEffect;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.lib.util.Effects;
import net.exylia.lib.task.TaskHandle;
import net.exylia.lib.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HoldEffectListener {

    private final ClassManager classManager;
    private TaskHandle task;

    public HoldEffectListener(ClassManager classManager) {
        this.classManager = classManager;
    }

    public void start() {
        task = Tasks.of(ExyliaClasses.getInstance().getPlugin()).runTimer(0L, 10L, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (classManager.getStateManager().get(player)
                        .filter(s -> s.isInClass() && !s.getCurrentClass().getHoldEffects().isEmpty())
                        .isEmpty()) continue;

                Tasks.of(ExyliaClasses.getInstance().getPlugin()).runAtEntity(player, () -> {
                    classManager.getStateManager().get(player).ifPresent(state -> {
                        if (!state.isInClass()) return;
                        ClassDefinition classDef = state.getCurrentClass();
                        Map<Material, HoldEffect> holdEffects = classDef.getHoldEffects();
                        if (holdEffects.isEmpty()) return;
                        Material held = player.getInventory().getItemInMainHand().getType();
                        HoldEffect holdEffect = holdEffects.get(held);
                        if (holdEffect == null) return;

                        List<Player> targets = ClanHelper.getTargets(
                                player, holdEffect.getType(), holdEffect.getRadius());
                        for (Player target : targets) {
                            if (target.equals(player)) {
                                Effects.apply(target, holdEffect.getEffects());
                            } else {
                                // A target near a region border is owned by another
                                // thread, so it gets its own hop instead of ours.
                                Tasks.of(ExyliaClasses.getInstance().getPlugin())
                                        .runAtEntity(target, () -> Effects.apply(target, holdEffect.getEffects()));
                            }
                        }
                    });
                });
            }
        });
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
