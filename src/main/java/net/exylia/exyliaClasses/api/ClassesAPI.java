package net.exylia.exyliaClasses.api;

import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public class ClassesAPI {

    private final ClassManager classManager;

    public ClassesAPI(ClassManager classManager) {
        this.classManager = classManager;
    }

    public boolean isInClass(Player player) {
        return classManager.getStateManager().get(player)
                .map(PlayerState::isInClass)
                .orElse(false);
    }

    public Optional<ClassDefinition> getCurrentClass(Player player) {
        return classManager.getStateManager().get(player)
                .filter(PlayerState::isInClass)
                .map(PlayerState::getCurrentClass);
    }

    public Optional<PlayerState> getPlayerState(Player player) {
        return classManager.getStateManager().get(player);
    }

    public double getPlayerEnergy(Player player) {
        return classManager.getStateManager().get(player)
                .map(PlayerState::getEnergy)
                .orElse(0.0);
    }

    public int getPlayerMarks(Player player) {
        return classManager.getStateManager().get(player)
                .map(PlayerState::getMarkCount)
                .orElse(0);
    }

    public Collection<ClassDefinition> getAllClasses() {
        return classManager.getAllClasses();
    }

    public Optional<ClassDefinition> getClass(String id) {
        return classManager.getClass(id);
    }

    public void removeFromClass(Player player) {
        classManager.removeClass(player);
    }
}
