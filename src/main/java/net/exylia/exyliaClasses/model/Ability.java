package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.block.Action;

import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class Ability {
    private final String displayName;
    private final Material material;
    private final AbilityType type;
    private final Set<Action> actions;
    private final List<String> effects;
    private final int cooldown;
    private final double energyCost;
    private final double radius;
}
