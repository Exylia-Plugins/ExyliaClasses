package net.exylia.exyliaClasses.manager;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<Material, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(Player player, Material material) {
        Map<Material, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(material);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public void setCooldown(Player player, Material material, int seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(material, System.currentTimeMillis() + (seconds * 1000L));
    }

    public long getRemainingSeconds(Player player, Material material) {
        Map<Material, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(material);
        if (expiry == null || expiry <= System.currentTimeMillis()) return 0;
        return (expiry - System.currentTimeMillis()) / 1000 + 1;
    }

    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}
