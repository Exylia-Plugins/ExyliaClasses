package net.exylia.exyliaClasses.model;

public enum AbilityType {
    SELF,
    TO_CLAN,
    TO_ALLY,
    TO_ENEMY,
    TO_CLAN_AND_ALLY;

    public static AbilityType fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SELF;
        }
    }
}
