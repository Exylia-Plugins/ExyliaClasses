package net.exylia.exyliaClasses.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.concurrent.TimeUnit;

public class ArmorListener implements Listener {

    private final ClassManager classManager;

    public ArmorListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TaskAPI.atLater(player, () -> classManager.checkArmorChange(player), 100L, TimeUnit.MILLISECONDS);
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
        TaskAPI.atLater(player, () -> classManager.checkArmorChange(player), 100L, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResurrect(EntityResurrectEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)) return;
        TaskAPI.atLater(player, () -> classManager.reapplyPassiveEffects(player), 50L, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        classManager.checkArmorChange(player);
    }
}
