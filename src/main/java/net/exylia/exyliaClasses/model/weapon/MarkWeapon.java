package net.exylia.exyliaClasses.model.weapon;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Map;
import java.util.TreeMap;

@Getter
public class MarkWeapon extends Weapon {
    private final int markDuration;
    private final boolean giveGlow;
    private final boolean markSameClass;
    private final TreeMap<Integer, MarkLevel> levels;

    public MarkWeapon(Material material, int markDuration, boolean giveGlow, boolean markSameClass,
                      Map<Integer, MarkLevel> levels) {
        super(material, WeaponType.MARK);
        this.markDuration = markDuration;
        this.giveGlow = giveGlow;
        this.markSameClass = markSameClass;
        this.levels = new TreeMap<>(levels);
    }

    public MarkLevel getLevel(int marks) {
        if (levels.isEmpty()) return null;
        int clamped = Math.min(marks, levels.lastKey());
        return levels.get(clamped);
    }

    public int getMaxLevel() {
        return levels.isEmpty() ? 0 : levels.lastKey();
    }
}
