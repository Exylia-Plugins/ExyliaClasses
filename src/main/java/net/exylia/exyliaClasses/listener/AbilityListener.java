package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.WarmupManager;
import net.exylia.exyliaClasses.model.Ability;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.lib.client.Clients;
import net.exylia.lib.client.Cooldown;
import net.exylia.lib.util.ItemCooldowns;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AbilityListener implements Listener {

    private final ClassManager classManager;

    public AbilityListener(ClassManager classManager) {
        this.classManager = classManager;
        ClassesMessages.load(ExyliaClasses.getInstance());
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

        ClassDefinition classDef = stateOpt.get().getCurrentClass();
        Material material = item.getType();
        Ability ability = classDef.getAbilities().get(material);
        if (ability == null) return;

        event.setCancelled(true);

        if (ItemCooldowns.isActive(player, material)) {
            WarmupManager.sendMessage(player, ClassesMessages.load(ExyliaClasses.getInstance()).abilities().cooldown(),
                    Map.of("ability", ability.getDisplayName(),
                            "time", ItemCooldowns.remainingFormatted(player, material)));
            return;
        }

        PlayerState state = stateOpt.get();

        double energyCost = ability.getEnergyCost();
        if (energyCost > 0 && state.getEnergy() < energyCost) {
            WarmupManager.sendMessage(player, ClassesMessages.load(ExyliaClasses.getInstance()).abilities().noEnergy(),
                    Map.of("needed", String.format("%.0f", energyCost),
                            "current", String.format("%.1f", state.getEnergy())));
            return;
        }

        if (energyCost > 0) {
            state.setEnergy(state.getEnergy() - energyCost);
        }

        if (ability.getCooldown() > 0) {
            Duration duration = Duration.ofSeconds(ability.getCooldown());
            ItemCooldowns.start(player, material, duration);
            Clients.cooldowns().show(player,
                    Cooldown.of("ability_" + material.name(), duration)
                            .icon(Cooldown.Icon.item(material.name())));
        }

        List<Player> targets = ClanHelper.getTargets(player, ability.getType(), ability.getRadius());

        List<String> effects = ability.getEffects();
        for (Player target : targets) {
            if (!target.isOnline()) continue;
            net.exylia.lib.util.Effects.apply(target, effects);
            if (ability.getParticles() != null) {
                classManager.showParticle(target, ability.getParticles());
            }
        }

        if (ability.getSound() != null) {
            classManager.playSound(player, ability.getSound());
        }

        WarmupManager.sendMessage(player, ClassesMessages.load(ExyliaClasses.getInstance()).abilities().used(),
                Map.of("ability", ability.getDisplayName()));

        for (Player target : targets) {
            if (target.equals(player)) continue;
            notifyTarget(target, player, ability);
        }

        consumeItem(player, item, material);
    }

    private void consumeItem(Player player, ItemStack item, Material material) {
        if (item.getType() != material) return;
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void notifyTarget(Player target, Player caster, Ability ability) {
        String message = switch (ability.getType()) {
            case TO_CLAN, TO_CLAN_AND_ALLY, TO_ALLY -> ClassesMessages.load(ExyliaClasses.getInstance()).abilities().clan();
            case TO_ENEMY -> ClassesMessages.load(ExyliaClasses.getInstance()).abilities().enemy();
            default -> null;
        };
        if (message == null) return;
        WarmupManager.sendMessage(target, message,
                Map.of("player", caster.getName(), "ability", ability.getDisplayName()));
    }
}
