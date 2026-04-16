package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Ability {
    private final String displayName;
    private final Material material;
    private final AbilityType type;
    private final List<String> effects;
    private final int cooldown;
    private final double energyCost;
    private final double radius;
    private final String particles;
    private final String sound;
}
