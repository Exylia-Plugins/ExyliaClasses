package net.exylia.exyliaClasses.util;

import net.exylia.exyliaClasses.model.AbilityType;
import net.exylia.lib.clan.Clans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ClanHelper {

    private ClanHelper() {}

    public static List<Player> getTargets(Player caster, AbilityType type, double radius) {
        return switch (type) {
            case SELF -> Collections.singletonList(caster);
            case TO_CLAN -> getClanMembers(caster, radius);
            case TO_CLAN_AND_ALLY -> getClanMembers(caster, radius);
            case TO_ALLY -> Collections.emptyList();
            case TO_ENEMY -> getEnemies(caster, radius);
        };
    }

    public static List<Player> getClanMembers(Player caster, double radius) {
        List<Player> targets = new ArrayList<>();
        targets.add(caster);

        var clanOpt = Clans.clanOf(caster);
        if (clanOpt.isEmpty()) return targets;

        double r2 = radius * radius;

        for (UUID memberId : Clans.onlineMembersOf(caster.getUniqueId())) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) continue;
            if (member.equals(caster)) continue;
            if (!caster.getWorld().equals(member.getWorld())) continue;
            if (caster.getLocation().distanceSquared(member.getLocation()) > r2) continue;
            targets.add(member);
        }
        return targets;
    }

    public static List<Player> getEnemies(Player caster, double radius) {
        List<Player> targets = new ArrayList<>();
        double r2 = radius * radius;
        for (Player nearby : caster.getWorld().getPlayers()) {
            if (nearby.equals(caster)) continue;
            if (caster.getLocation().distanceSquared(nearby.getLocation()) > r2) continue;
            if (Clans.areInSameClan(caster, nearby)) continue;
            targets.add(nearby);
        }
        return targets;
    }
}
