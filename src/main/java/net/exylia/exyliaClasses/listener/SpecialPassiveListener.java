package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.visual.api.EffectAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpecialPassiveListener {

    private final ClassManager classManager;
    private ScheduledTask task;

    public SpecialPassiveListener(ClassManager classManager) {
        this.classManager = classManager;
    }

    public void start() {
        task = TaskAPI.syncTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (classManager.getStateManager().get(player)
                        .filter(s -> s.isInClass() && !s.getCurrentClass().getSpecialPassives().isEmpty())
                        .isEmpty()) continue;

                TaskAPI.atLater(player, () -> {
                    classManager.getStateManager().get(player).ifPresent(state -> {
                        if (!state.isInClass()) return;
                        ClassDefinition classDef = state.getCurrentClass();

                        List<SpecialPassive> specialPassives = classDef.getSpecialPassives();
                        if (specialPassives.isEmpty()) return;

                        for (SpecialPassive sp : specialPassives) {
                            boolean conditionMet = sp.check(player);
                            boolean wasActive = state.getActiveSpecialPassives().contains(sp);

                            if (conditionMet && !wasActive) {
                                EffectAPI.applyAllInfinite(player, sp.getEffects());
                                state.getActiveSpecialPassives().add(sp);
                            } else if (!conditionMet && wasActive) {
                                EffectAPI.removeAll(player, sp.getEffects());
                                state.getActiveSpecialPassives().remove(sp);
                            }
                        }
                    });
                }, 0L, TimeUnit.MILLISECONDS);
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
