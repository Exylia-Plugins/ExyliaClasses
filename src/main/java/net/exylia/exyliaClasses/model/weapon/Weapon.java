package net.exylia.exyliaClasses.model.weapon;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public abstract class Weapon {
    private final Material material;
    private final WeaponType type;

    protected Weapon(Material material, WeaponType type) {
        this.material = material;
        this.type = type;
    }
}
