package net.exylia.exyliaClasses.model.weapon;

import lombok.Getter;
import java.util.List;

@Getter
public class MarkLevel {
    private final int level;
    private final double extraDamage;
    private final String color;
    private final String icon;
    private final boolean removeInvisibility;
    private final List<String> attackedEffects;
    private final List<String> attackerEffects;
    private final String attackedParticles;
    private final String attackerParticles;
    private final String attackedSound;
    private final String attackerSound;

    public MarkLevel(int level, double extraDamage, String color, String icon, boolean removeInvisibility,
                     List<String> attackedEffects, List<String> attackerEffects,
                     String attackedParticles, String attackerParticles,
                     String attackedSound, String attackerSound) {
        this.level = level;
        this.extraDamage = extraDamage;
        this.color = color;
        this.icon = icon;
        this.removeInvisibility = removeInvisibility;
        this.attackedEffects = attackedEffects;
        this.attackerEffects = attackerEffects;
        this.attackedParticles = attackedParticles;
        this.attackerParticles = attackerParticles;
        this.attackedSound = attackedSound;
        this.attackerSound = attackerSound;
    }
}
