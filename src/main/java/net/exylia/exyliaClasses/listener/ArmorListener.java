package net.exylia.exyliaClasses.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.lib.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ArmorListener implements Listener {

    private final ClassManager classManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public ArmorListener(ClassManager classManager) {
        this.classManager = classManager;
        this.plugin = ExyliaClasses.getInstance().getPlugin();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> {
            classManager.restoreCooldownOverlays(player);
            classManager.checkArmorChange(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        classManager.removeAll(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        classManager.removeAll(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> {
            classManager.restoreCooldownOverlays(player);
            classManager.checkArmorChange(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResurrect(EntityResurrectEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)) return;
        Tasks.of(plugin).runAtEntity(player, () ->
            classManager.reapplyPassiveEffects(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        classManager.checkArmorChange(event.getPlayer());
    }
}
