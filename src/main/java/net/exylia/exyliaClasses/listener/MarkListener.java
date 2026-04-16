package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.exyliaClasses.model.weapon.MarkWeapon;
import net.exylia.exyliaClasses.model.weapon.WeaponType;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

public class MarkListener implements Listener {

    private final ClassManager classManager;

    public MarkListener(ClassManager classManager) {
        this.classManager = classManager;
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        Optional<PlayerState> attackerStateOpt = classManager.getStateManager().get(attacker);
        if (attackerStateOpt.isEmpty() || !attackerStateOpt.get().isInClass()) return;

        ClassDefinition classDef = attackerStateOpt.get().getCurrentClass();
        MarkWeapon markWeapon = classDef.getWeapons().values().stream()
                .filter(w -> w.getType() == WeaponType.MARK)
                .map(w -> (MarkWeapon) w)
                .findFirst().orElse(null);
        if (markWeapon == null) return;

        if (event.getDamager() instanceof Arrow) {
            if (!markWeapon.isMarkSameClass()) {
                Optional<PlayerState> targetStateOpt = classManager.getStateManager().get(target);
                if (targetStateOpt.isPresent() && targetStateOpt.get().isInClass()) {
                    if (classDef.getId().equals(targetStateOpt.get().getCurrentClass().getId())) {
                        MessageAPI.send(attacker, MessagesDefaults.Weapons.Mark.SAME_CLASS);
                        return;
                    }
                }
            }
            applyMark(attacker, target, markWeapon);
        }

        Optional<PlayerState> targetStateOpt = classManager.getStateManager().get(target);
        if (targetStateOpt.isEmpty() || !targetStateOpt.get().isMarked()) return;

        PlayerState targetState = targetStateOpt.get();
        MarkLevel level = markWeapon.getLevel(targetState.getMarkCount());
        if (level == null) return;

        double bonus = event.getDamage() * (level.getExtraDamage() / 100.0);
        event.setDamage(event.getDamage() + bonus);

        EffectAPI.applyAll(target, level.getAttackedEffects());
        EffectAPI.applyAll(attacker, level.getAttackerEffects());
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Arrow arrow) {
            ProjectileSource shooter = arrow.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private void applyMark(Player shooter, Player target, MarkWeapon markWeapon) {
        PlayerState targetState = classManager.getStateManager().getOrCreate(target);

        int newCount = Math.min(targetState.getMarkCount() + 1, markWeapon.getMaxLevel());
        targetState.setMarkCount(newCount);
        targetState.setMarkWeapon(markWeapon);

        if (markWeapon.isGiveGlow()) {
            MarkLevel level = markWeapon.getLevel(newCount);
            if (level != null && level.isRemoveInvisibility()) {
                target.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            target.setGlowing(true);
        }

        classManager.createMarkBossBar(target, targetState);

        MarkLevel level = markWeapon.getLevel(newCount);
        String damage = level != null ? String.valueOf((int) level.getExtraDamage()) : "?";

        MessageAPI.send(shooter, MessagesDefaults.Weapons.Mark.APPLIED, PlaceholderContext.create()
                .put("target", target.getName())
                .put("damage", damage));
        MessageAPI.send(target, MessagesDefaults.Weapons.Mark.RECEIVED, PlaceholderContext.create()
                .put("attacker", shooter.getName())
                .put("damage", damage));
    }
}
