package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.lib.util.Effects;
import net.exylia.lib.task.TaskHandle;
import net.exylia.lib.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class SpecialPassiveListener {

    private final ClassManager classManager;
    private TaskHandle task;

    public SpecialPassiveListener(ClassManager classManager) {
        this.classManager = classManager;
    }

    public void start() {
        task = Tasks.of(ExyliaClasses.getInstance().getPlugin()).runTimer(0L, 20L, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (classManager.getStateManager().get(player)
                        .filter(s -> s.isInClass() && !s.getCurrentClass().getSpecialPassives().isEmpty())
                        .isEmpty()) continue;

                Tasks.of(ExyliaClasses.getInstance().getPlugin()).runAtEntity(player, () -> {
                    classManager.getStateManager().get(player).ifPresent(state -> {
                        if (!state.isInClass()) return;
                        ClassDefinition classDef = state.getCurrentClass();
                        List<SpecialPassive> passives = classDef.getSpecialPassives();
                        if (passives.isEmpty()) return;

                        for (SpecialPassive sp : passives) {
                            boolean met = sp.check(player);
                            boolean was = state.getActiveSpecialPassives().contains(sp);

                            if (met && !was) {
                                Effects.applyInfinite(player, sp.getEffects());
                                state.getActiveSpecialPassives().add(sp);
                            } else if (!met && was) {
                                Effects.remove(player, sp.getEffects());
                                state.getActiveSpecialPassives().remove(sp);
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
