package net.exylia.exyliaClasses.util;

import net.exylia.exyliaClasses.model.AbilityType;
import net.exylia.commons.v2.clan.api.ClanAPI;
import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        Optional<Clan> clanOpt = ClanAPI.getPlayerClan(caster);
        if (clanOpt.isEmpty()) return Collections.emptyList();

        Clan clan = clanOpt.get();
        List<Player> targets = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (UUID memberId : clan.getOnlineMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) continue;
            if (!caster.getWorld().equals(member.getWorld())) continue;
            if (caster.getLocation().distanceSquared(member.getLocation()) > radiusSquared) continue;
            targets.add(member);
        }
        return targets;
    }

    public static List<Player> getEnemies(Player caster, double radius) {
        List<Player> targets = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (Player nearby : caster.getWorld().getPlayers()) {
            if (nearby.equals(caster)) continue;
            if (caster.getLocation().distanceSquared(nearby.getLocation()) > radiusSquared) continue;
            if (ClanAPI.isSameClan(caster, nearby)) continue;
            targets.add(nearby);
        }
        return targets;
    }
}
