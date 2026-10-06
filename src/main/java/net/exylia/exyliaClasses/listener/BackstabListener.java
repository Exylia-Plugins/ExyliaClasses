package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.WarmupManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.BackstabWeapon;
import net.exylia.exyliaClasses.model.weapon.WeaponType;
import net.exylia.lib.format.Numbers;
import net.exylia.lib.util.Effects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Optional;

public class BackstabListener implements Listener {

    private final ClassManager classManager;

    public BackstabListener(ClassManager classManager) {
        this.classManager = classManager;
        ClassesMessages.load(ExyliaClasses.getInstance());
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
        if (attacker.getInventory().getItemInMainHand().getType() != backstab.getMaterial()) return;

        float attackCooldown = attacker.getAttackCooldown();
        if (attackCooldown < backstab.getAttackCooldownNeed()) {
            WarmupManager.sendMessage(attacker, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().backstab().noCooldown(),
                    Map.of("current", Numbers.decimals(attackCooldown * 100, 0),
                            "needed", Numbers.decimals(backstab.getAttackCooldownNeed() * 100, 0)));
            return;
        }

        if (!isBackstab(attacker, target)) return;

        double newHealth = target.getHealth() - backstab.getExtraDamage();

        if (!backstab.isCanDead() && newHealth <= 0) {
            WarmupManager.sendMessage(attacker, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().backstab().cantDead(), null);
            return;
        }

        event.setDamage(0);
        target.setHealth(Math.max(0, newHealth));

        Effects.apply(target, backstab.getAttackedEffects());
        Effects.apply(attacker, backstab.getAttackerEffects());

        if (backstab.getAttackedSound() != null)
            classManager.playSound(target, backstab.getAttackedSound());
        if (backstab.getAttackedParticles() != null)
            classManager.showParticle(target, backstab.getAttackedParticles());
        if (backstab.getAttackerSound() != null)
            classManager.playSound(attacker, backstab.getAttackerSound());
        if (backstab.getAttackerParticles() != null)
            classManager.showParticle(attacker, backstab.getAttackerParticles());

        if (backstab.isBreakItem()) {
            attacker.getInventory().setItemInMainHand(null);
        }

        WarmupManager.sendMessage(attacker, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().backstab().success(), null);
        WarmupManager.sendMessage(target, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().backstab().victim(),
                Map.of("attacker", attacker.getName()));
    }

    private boolean isBackstab(Player attacker, Player target) {
        Vector toAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();
        Vector targetFacing = target.getLocation().getDirection().normalize();
        return targetFacing.dot(toAttacker) < -0.3;
    }
}
