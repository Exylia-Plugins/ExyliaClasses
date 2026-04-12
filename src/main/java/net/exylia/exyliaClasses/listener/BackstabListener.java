package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.BackstabWeapon;
import net.exylia.exyliaClasses.model.weapon.WeaponType;
import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Optional;

public class BackstabListener implements Listener {

    private final ClassManager classManager;

    public BackstabListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Optional<PlayerState> stateOpt = classManager.getStateManager().get(attacker);
        if (stateOpt.isEmpty() || !stateOpt.get().isInClass()) return;

        ClassDefinition classDef = stateOpt.get().getCurrentClass();
        BackstabWeapon backstab = classDef.getWeapons().values().stream()
                .filter(w -> w.getType() == WeaponType.BACKSTAB)
                .map(w -> (BackstabWeapon) w)
                .findFirst().orElse(null);
        if (backstab == null) return;

        float attackCooldown = attacker.getAttackCooldown();
        if (attackCooldown < backstab.getAttackCooldownNeed()) {
            MessageAPI.send(attacker, Messages.get("weapons.backstab.no-cooldown",
                    "%current%", String.format("%.0f", attackCooldown * 100),
                    "%needed%", String.format("%.0f", backstab.getAttackCooldownNeed() * 100)));
            return;
        }

        if (!isBackstab(attacker, target)) return;

        double currentDamage = event.getDamage();
        double extraDamage = currentDamage * (backstab.getExtraDamage() / 100.0);
        double finalDamage = currentDamage + extraDamage;

        if (!backstab.isCanDead()) {
            double targetHealth = target.getHealth();
            if (finalDamage >= targetHealth) {
                MessageAPI.send(attacker, Messages.get("weapons.backstab.cant-dead"));
                finalDamage = Math.max(targetHealth - 0.5, currentDamage);
            }
        }

        event.setDamage(finalDamage);

        EffectAPI.applyAll(target, backstab.getAttackedEffects());
        EffectAPI.applyAll(attacker, backstab.getAttackerEffects());

        if (backstab.isBreakItem()) {
            attacker.getInventory().getItemInMainHand().setDurability(Short.MAX_VALUE);
        }

        MessageAPI.send(attacker, Messages.get("weapons.backstab.success"));
        MessageAPI.send(target, Messages.get("weapons.backstab.victim",
                "%attacker%", attacker.getName()));
    }

    private boolean isBackstab(Player attacker, Player target) {
        Vector toAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector())
                .normalize();
        Vector targetFacing = target.getLocation().getDirection().normalize();
        double dot = targetFacing.dot(toAttacker);
        return dot < -0.3;
    }
}
