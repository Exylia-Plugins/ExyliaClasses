package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class HoldEffect {
    private final Material material;
    private final AbilityType type;
    private final double radius;
    private final List<String> effects;
}
