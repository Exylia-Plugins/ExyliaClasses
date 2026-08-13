package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.exyliaClasses.model.weapon.MarkWeapon;
import net.exylia.lib.effect.Display;
import net.exylia.lib.task.TaskHandle;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class PlayerState {
    private final Player player;
    private ClassDefinition currentClass;

    private double energy = 0;
    private TaskHandle energyTask;
    private Display energyDisplay;

    private int markCount = 0;
    private MarkWeapon markWeapon;
    private Display markDisplay;

    private final Set<SpecialPassive> activeSpecialPassives = new HashSet<>();

    public PlayerState(Player player) {
        this.player = player;
    }

    public boolean isInClass() {
        return currentClass != null;
    }

    public boolean isMarked() {
        return markCount > 0 && markWeapon != null;
    }
}
