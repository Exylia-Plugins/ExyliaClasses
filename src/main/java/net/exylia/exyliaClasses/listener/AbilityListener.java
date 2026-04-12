package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.CooldownManager;
import net.exylia.exyliaClasses.model.Ability;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class AbilityListener implements Listener {

    private final ClassManager classManager;

    public AbilityListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        Optional<PlayerState> stateOpt = classManager.getStateManager().get(player);
        if (stateOpt.isEmpty() || !stateOpt.get().isInClass()) return;

        PlayerState state = stateOpt.get();
        ClassDefinition classDef = state.getCurrentClass();

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material material = item.getType();
        Ability ability = classDef.getAbilities().get(material);
        if (ability == null) return;

        if (!ability.getActions().contains(event.getAction())) return;

        event.setCancelled(true);

        CooldownManager cooldownManager = classManager.getCooldownManager();

        if (cooldownManager.isOnCooldown(player, material)) {
            long remaining = cooldownManager.getRemainingSeconds(player, material);
            MessageAPI.send(player, Messages.get("abilities.cooldown",
                    "%ability%", ability.getDisplayName(),
                    "%time%", remaining));
            return;
        }

        if (ability.getEnergyCost() > 0) {
            if (state.getEnergy() < ability.getEnergyCost()) {
                MessageAPI.send(player, Messages.get("abilities.no-energy",
                        "%needed%", (int) ability.getEnergyCost(),
                        "%current%", (int) state.getEnergy()));
                return;
            }
            state.setEnergy(state.getEnergy() - ability.getEnergyCost());
        }

        removeOneItem(player, material);

        cooldownManager.setCooldown(player, material, ability.getCooldown());
        player.setCooldown(material, ability.getCooldown() * 20);

        List<Player> targets = ClanHelper.getTargets(player, ability.getType(), ability.getRadius());
        for (Player target : targets) {
            EffectAPI.applyAll(target, ability.getEffects());

            if (!target.equals(player)) {
                notifyTarget(target, player, ability);
            }
        }

        MessageAPI.send(player, Messages.get("abilities.used", "%ability%", ability.getDisplayName()));
    }

    private void removeOneItem(Player player, Material material) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != material) return;

        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void notifyTarget(Player target, Player caster, Ability ability) {
        String route = switch (ability.getType()) {
            case TO_CLAN, TO_CLAN_AND_ALLY, TO_ALLY -> "abilities.clan";
            case TO_ENEMY -> "abilities.enemy";
            default -> null;
        };
        if (route == null) return;
        MessageAPI.send(target, Messages.get(route,
                "%player%", caster.getName(),
                "%ability%", ability.getDisplayName()));
    }
}
