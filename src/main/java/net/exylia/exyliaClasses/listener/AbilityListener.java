package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.CooldownManager;
import net.exylia.exyliaClasses.model.Ability;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AbilityListener implements Listener {

    private final ClassManager classManager;
    private final Set<UUID> processingThisTick = new HashSet<>();

    public AbilityListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
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

        if (!processingThisTick.add(player.getUniqueId())) return;
        ExyliaClasses.getInstance().getPlugin().getServer().getScheduler()
                .runTask(ExyliaClasses.getInstance().getPlugin(), () -> processingThisTick.remove(player.getUniqueId()));

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        event.setCancelled(true);

        CooldownManager cooldownManager = classManager.getCooldownManager();

        if (cooldownManager.isOnCooldown(player, material)) {
            long remaining = cooldownManager.getRemainingSeconds(player, material);
            MessageAPI.send(player, MessagesDefaults.Abilities.COOLDOWN, PlaceholderContext.create()
                    .put("ability", ability.getDisplayName())
                    .put("time", remaining));
            return;
        }

        if (ability.getEnergyCost() > 0) {
            if (state.getEnergy() < ability.getEnergyCost()) {
                MessageAPI.send(player, MessagesDefaults.Abilities.NO_ENERGY, PlaceholderContext.create()
                        .put("needed", (int) ability.getEnergyCost())
                        .put("current", (int) state.getEnergy()));
                return;
            }
            state.setEnergy(state.getEnergy() - ability.getEnergyCost());
        }

        removeOneItem(player, material);

        cooldownManager.setCooldown(player, material, ability.getCooldown());
        player.setCooldown(material, ability.getCooldown() * 20);

        if (ability.getSound() != null) SoundAPI.play(player, ability.getSound());
        if (ability.getParticles() != null)
            ParticleAPI.spawn(player.getLocation(), ParticleBuilder.fromString(ability.getParticles()));

        DebugAPI.logPluginDebug("[Ability] " + player.getName() + " used " + material + " | type=" + ability.getType() + " radius=" + ability.getRadius() + " effects=" + ability.getEffects());
        List<Player> targets = ClanHelper.getTargets(player, ability.getType(), ability.getRadius());
        DebugAPI.logPluginDebug("[Ability] targets: " + targets.stream().map(Player::getName).toList());

        List<String> effects = ability.getEffects();
        String particles = ability.getParticles();
        for (Player target : targets) {
            TaskAPI.atLater(target, () -> {
                EffectAPI.applyAll(target, effects);
                if (particles != null) {
                    ParticleAPI.spawn(target.getLocation(), ParticleBuilder.fromString(particles));
                }
                if (!target.equals(player)) notifyTarget(target, player, ability);
            }, 0L, TimeUnit.MILLISECONDS);
        }

        MessageAPI.send(player, MessagesDefaults.Abilities.USED, PlaceholderContext.create()
                .put("ability", ability.getDisplayName()));
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
        String message = switch (ability.getType()) {
            case TO_CLAN, TO_CLAN_AND_ALLY, TO_ALLY -> MessagesDefaults.Abilities.CLAN;
            case TO_ENEMY -> MessagesDefaults.Abilities.ENEMY;
            default -> null;
        };
        if (message == null) return;
        MessageAPI.send(target, message, PlaceholderContext.create()
                .put("player", caster.getName())
                .put("ability", ability.getDisplayName()));
    }
}
