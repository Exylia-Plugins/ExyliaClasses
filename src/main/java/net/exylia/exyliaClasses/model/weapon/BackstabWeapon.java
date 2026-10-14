package net.exylia.exyliaClasses.model.weapon;

import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

@Getter
public class BackstabWeapon extends Weapon {
    private final double extraDamage;
    private final boolean canDead;
    private final double attackCooldownNeed;
    private final boolean breakItem;
    private final List<String> attackedEffects;
    private final List<String> attackerEffects;
    private final String attackedParticles;
    private final String attackerParticles;
    private final String attackedSound;
    private final String attackerSound;

    public BackstabWeapon(Material material, double extraDamage, boolean canDead,
                          double attackCooldownNeed, boolean breakItem,
                          List<String> attackedEffects, List<String> attackerEffects,
                          String attackedParticles, String attackerParticles,
                          String attackedSound, String attackerSound) {
        super(material, WeaponType.BACKSTAB);
        this.extraDamage = extraDamage;
        this.canDead = canDead;
        this.attackCooldownNeed = attackCooldownNeed;
        this.breakItem = breakItem;
        this.attackedEffects = attackedEffects;
        this.attackerEffects = attackerEffects;
        this.attackedParticles = attackedParticles;
        this.attackerParticles = attackerParticles;
        this.attackedSound = attackedSound;
        this.attackerSound = attackerSound;
    }
}
