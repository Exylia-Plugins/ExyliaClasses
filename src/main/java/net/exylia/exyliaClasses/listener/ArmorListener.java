package net.exylia.exyliaClasses.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.lib.respawn.Respawns;
import net.exylia.lib.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ArmorListener implements Listener {

    private final ClassManager classManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public ArmorListener(ClassManager classManager) {
        this.classManager = classManager;
        this.plugin = ExyliaClasses.getInstance().getPlugin();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Respawns.of(plugin).after(player -> {
            classManager.restoreCooldownOverlays(player);
            classManager.checkArmorChange(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // On the player's own thread: the global region scheduler is the wrong thread on Folia.
        net.exylia.lib.task.Tasks.of(plugin).runAtEntityLater(player, 2L, () -> {
            classManager.restoreCooldownOverlays(player);
            classManager.checkArmorChange(player);
        });
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
