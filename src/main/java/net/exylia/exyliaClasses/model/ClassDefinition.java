package net.exylia.exyliaClasses.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.exyliaClasses.model.weapon.Weapon;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ClassDefinition {
    private final String id;
    private final String displayName;
    private final String permission;
    private final Map<EquipmentSlot, Material> equipment;
    private final List<String> passiveEffects;
    private final List<SpecialPassive> specialPassives;
    private final Warmup warmup;
    private final Energy energy;
    private final Map<Material, Ability> abilities;
    private final Map<Material, HoldEffect> holdEffects;
    private final Map<Material, Weapon> weapons;

    public boolean hasPermission() {
        return permission != null && !permission.equalsIgnoreCase("NONE") && !permission.isBlank();
    }
}
