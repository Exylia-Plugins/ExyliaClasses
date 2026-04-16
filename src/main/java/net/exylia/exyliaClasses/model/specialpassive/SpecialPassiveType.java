package net.exylia.exyliaClasses.model.specialpassive;

import org.bukkit.entity.Player;

public enum SpecialPassiveType {

    BELOW_Y {
        @Override
        public boolean check(Player player, String condition) {
            try {
                return player.getLocation().getY() < Double.parseDouble(condition);
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    public abstract boolean check(Player player, String condition);

    public static SpecialPassiveType fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
