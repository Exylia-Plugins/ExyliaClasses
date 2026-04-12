package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.model.PlayerState;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    public PlayerState getOrCreate(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState(player));
    }

    public Optional<PlayerState> get(Player player) {
        return Optional.ofNullable(states.get(player.getUniqueId()));
    }

    public Optional<PlayerState> get(UUID uuid) {
        return Optional.ofNullable(states.get(uuid));
    }

    public void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    public boolean has(Player player) {
        return states.containsKey(player.getUniqueId());
    }

    public Map<UUID, PlayerState> getAll() {
        return Collections.unmodifiableMap(states);
    }
}
