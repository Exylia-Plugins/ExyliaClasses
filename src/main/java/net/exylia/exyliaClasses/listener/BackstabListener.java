package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.BackstabWeapon;
import net.exylia.exyliaClasses.model.weapon.WeaponType;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
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
            MessageAPI.send(attacker, MessagesDefaults.Weapons.Backstab.NO_COOLDOWN, PlaceholderContext.create()
                    .put("current", String.format("%.0f", attackCooldown * 100))
                    .put("needed", String.format("%.0f", backstab.getAttackCooldownNeed() * 100)));
            return;
        }

        if (!isBackstab(attacker, target)) return;

        double newHealth = target.getHealth() - backstab.getExtraDamage();

        if (!backstab.isCanDead() && newHealth <= 0) {
            MessageAPI.send(attacker, MessagesDefaults.Weapons.Backstab.CANT_DEAD);
            return;
        }

        event.setDamage(0);
        target.setHealth(Math.max(0, newHealth));

        EffectAPI.applyAll(target, backstab.getAttackedEffects());
        EffectAPI.applyAll(attacker, backstab.getAttackerEffects());

        if (backstab.getAttackedSound() != null) SoundAPI.play(target, backstab.getAttackedSound());
        if (backstab.getAttackedParticles() != null)
            ParticleAPI.spawn(target.getLocation(), ParticleBuilder.fromString(backstab.getAttackedParticles()));
        if (backstab.getAttackerSound() != null) SoundAPI.play(attacker, backstab.getAttackerSound());
        if (backstab.getAttackerParticles() != null)
            ParticleAPI.spawn(attacker.getLocation(), ParticleBuilder.fromString(backstab.getAttackerParticles()));

        if (backstab.isBreakItem()) {
            attacker.getInventory().setItemInMainHand(null);
        }

        MessageAPI.send(attacker, MessagesDefaults.Weapons.Backstab.SUCCESS);
        MessageAPI.send(target, MessagesDefaults.Weapons.Backstab.VICTIM, PlaceholderContext.create()
                .put("attacker", attacker.getName()));
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
