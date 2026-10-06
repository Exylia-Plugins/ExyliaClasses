package net.exylia.exyliaClasses.placeholder;

import net.exylia.exyliaClasses.api.ClassesAPI;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.lib.format.Numbers;
import net.exylia.lib.placeholder.Placeholders;
import org.bukkit.plugin.Plugin;

public class ClassesPlaceholders {

    private final ClassesAPI api;

    public ClassesPlaceholders(ClassesAPI api) {
        this.api = api;
    }

    public void register(Plugin plugin) {
        Placeholders.group(plugin, "classes")
                .add("class", r -> playerClass(r.viewer()))
                .add("class_id", r -> playerClassId(r.viewer()))
                .add("energy", r -> playerEnergy(r.viewer()))
                .add("max_energy", r -> playerMaxEnergy(r.viewer()))
                .add("energy_symbol", r -> playerEnergySymbol(r.viewer()))
                .add("marks", r -> playerMarks(r.viewer()))
                .add("in_class", r -> playerInClass(r.viewer()))
                .add("marked", r -> playerMarked(r.viewer()))
                .add("mark_color", r -> markColor(r.viewer()))
                .add("mark_icon", r -> markIcon(r.viewer()))
                .add("mark_display", r -> markDisplay(r.viewer()))
                .add("mark_display_space", r -> markDisplaySpace(r.viewer()))
                .add("mark_extra_damage", r -> markExtraDamage(r.viewer()))
                .add("mark_level", r -> markLevel(r.viewer()))
                .register();
    }

    private String playerClass(org.bukkit.entity.Player player) {
        return api.getCurrentClass(player).map(ClassDefinition::getDisplayName).orElse("Ninguna");
    }

    private String playerClassId(org.bukkit.entity.Player player) {
        return api.getCurrentClass(player).map(ClassDefinition::getId).orElse("none");
    }

    private String playerEnergy(org.bukkit.entity.Player player) {
        return api.getPlayerState(player).map(s -> Numbers.decimals(s.getEnergy(), 1)).orElse("0");
    }

    private String playerMaxEnergy(org.bukkit.entity.Player player) {
        return api.getCurrentClass(player).map(ClassDefinition::getEnergy).map(e -> String.valueOf((int) e.getMax())).orElse("0");
    }

    private String playerEnergySymbol(org.bukkit.entity.Player player) {
        return api.getCurrentClass(player).map(ClassDefinition::getEnergy).map(Energy::getSymbol).orElse("");
    }

    private String playerMarks(org.bukkit.entity.Player player) {
        return api.getPlayerState(player).map(s -> String.valueOf(s.getMarkCount())).orElse("0");
    }

    private String playerInClass(org.bukkit.entity.Player player) {
        return String.valueOf(api.isInClass(player));
    }

    private String playerMarked(org.bukkit.entity.Player player) {
        return api.getPlayerState(player).map(s -> String.valueOf(s.isMarked())).orElse("false");
    }

    private String markColor(org.bukkit.entity.Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() : "";
    }

    private String markIcon(org.bukkit.entity.Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getIcon() : "";
    }

    private String markDisplay(org.bukkit.entity.Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() + level.getIcon() : "";
    }

    private String markDisplaySpace(org.bukkit.entity.Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? level.getColor() + level.getIcon() + " " : "";
    }

    private String markExtraDamage(org.bukkit.entity.Player player) {
        MarkLevel level = getMarkLevel(player);
        return level != null ? String.valueOf((int) level.getExtraDamage()) : "0";
    }

    private String markLevel(org.bukkit.entity.Player player) {
        return api.getPlayerState(player).filter(PlayerState::isMarked)
                .map(s -> String.valueOf(s.getMarkCount())).orElse("0");
    }

    private MarkLevel getMarkLevel(org.bukkit.entity.Player player) {
        return api.getPlayerState(player).filter(PlayerState::isMarked)
                .map(s -> s.getMarkWeapon().getLevel(s.getMarkCount())).orElse(null);
    }
}
