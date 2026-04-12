package net.exylia.exyliaClasses.placeholder;

import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.commons.v2.placeholders.annotation.Placeholder;
import net.exylia.commons.v2.placeholders.annotation.PlaceholderScope;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ClassesPlaceholders {

    private final ClassesAPI api;

    public ClassesPlaceholders(ClassesAPI api) {
        this.api = api;
    }

    @Placeholder(name = "class", scope = PlaceholderScope.PLAYER)
    public String playerClass(Player player) {
        return api.getCurrentClass(player)
                .map(ClassDefinition::getDisplayName)
                .orElse("Ninguna");
    }

    @Placeholder(name = "class_id", scope = PlaceholderScope.PLAYER)
    public String playerClassId(Player player) {
        return api.getCurrentClass(player)
                .map(ClassDefinition::getId)
                .orElse("none");
    }

    @Placeholder(name = "energy", scope = PlaceholderScope.PLAYER)
    public String playerEnergy(Player player) {
        Optional<PlayerState> state = api.getPlayerState(player);
        return state.map(s -> String.format("%.1f", s.getEnergy())).orElse("0");
    }

    @Placeholder(name = "max_energy", scope = PlaceholderScope.PLAYER)
    public String playerMaxEnergy(Player player) {
        return api.getCurrentClass(player)
                .map(ClassDefinition::getEnergy)
                .map(e -> String.valueOf((int) e.getMax()))
                .orElse("0");
    }

    @Placeholder(name = "energy_symbol", scope = PlaceholderScope.PLAYER)
    public String playerEnergySymbol(Player player) {
        return api.getCurrentClass(player)
                .map(ClassDefinition::getEnergy)
                .map(Energy::getSymbol)
                .orElse("");
    }

    @Placeholder(name = "marks", scope = PlaceholderScope.PLAYER)
    public String playerMarks(Player player) {
        return api.getPlayerState(player)
                .map(s -> String.valueOf(s.getMarkCount()))
                .orElse("0");
    }

    @Placeholder(name = "in_class", scope = PlaceholderScope.PLAYER)
    public String playerInClass(Player player) {
        return String.valueOf(api.isInClass(player));
    }
}
