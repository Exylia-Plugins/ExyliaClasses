package net.exylia.exyliaClasses.placeholder;

import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
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

    @Placeholder(name = "marked", scope = PlaceholderScope.PLAYER)
    public String playerMarked(Player player) {
        return api.getPlayerState(player)
                .map(s -> String.valueOf(s.isMarked()))
                .orElse("false");
    }

    @Placeholder(name = "mark_color", scope = PlaceholderScope.PLAYER)
    public String markColor(Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() : "";
    }

    @Placeholder(name = "mark_icon", scope = PlaceholderScope.PLAYER)
    public String markIcon(Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getIcon() : "";
    }

    @Placeholder(name = "mark_display", scope = PlaceholderScope.PLAYER)
    public String markDisplay(Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() + level.getIcon() : "";
    }

    @Placeholder(name = "mark_display_space", scope = PlaceholderScope.PLAYER)
    public String markDisplaySpace(Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() + level.getIcon() + " " : "";
    }

    @Placeholder(name = "mark_extra_damage", scope = PlaceholderScope.PLAYER)
    public String markExtraDamage(Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? String.valueOf((int) level.getExtraDamage()) : "0";
    }

    @Placeholder(name = "mark_level", scope = PlaceholderScope.PLAYER)
    public String markLevel(Player player) {
        return api.getPlayerState(player)
                .filter(PlayerState::isMarked)
                .map(s -> String.valueOf(s.getMarkCount()))
                .orElse("0");
    }

    private MarkLevel getMarkLevel(Player player) {
        return api.getPlayerState(player)
                .filter(PlayerState::isMarked)
                .map(s -> s.getMarkWeapon().getLevel(s.getMarkCount()))
                .orElse(null);
    }
}
