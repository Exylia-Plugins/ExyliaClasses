package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class AbilityListener implements Listener {

    private final ClassManager classManager;

    public AbilityListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        Optional<PlayerState> stateOpt = classManager.getStateManager().get(player);
        if (stateOpt.isEmpty() || !stateOpt.get().isInClass()) return;

        Material material = item.getType();
        if (!stateOpt.get().getCurrentClass().getAbilities().containsKey(material)) return;

        event.setCancelled(true);

        if (classManager.useAbility(player, material) == ClassManager.AbilityOutcome.USED) {
            consumeItem(player, item, material);
        }
    }

    private void consumeItem(Player player, ItemStack item, Material material) {
        if (item.getType() != material) return;
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
