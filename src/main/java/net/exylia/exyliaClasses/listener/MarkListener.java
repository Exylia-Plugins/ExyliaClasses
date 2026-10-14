package net.exylia.exyliaClasses.listener;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.manager.WarmupManager;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.exyliaClasses.model.weapon.MarkWeapon;
import net.exylia.exyliaClasses.model.weapon.WeaponType;
import net.exylia.lib.util.Effects;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Optional;

public class MarkListener implements Listener {

    private final ClassManager classManager;

    public MarkListener(ClassManager classManager) {
        this.classManager = classManager;
        ClassesMessages.load(ExyliaClasses.getInstance());
        ExyliaClasses.getInstance().getPlugin().getServer().getPluginManager()
                .registerEvents(this, ExyliaClasses.getInstance().getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;

        Player shooter = resolveShooter(arrow);
        if (shooter == null) return;
        if (shooter.equals(target)) return;

        Optional<PlayerState> stateOpt = classManager.getStateManager().get(shooter);
        if (stateOpt.isEmpty() || !stateOpt.get().isInClass()) return;

        ClassDefinition classDef = stateOpt.get().getCurrentClass();
        MarkWeapon markWeapon = classDef.getWeapons().values().stream()
                .filter(w -> w.getType() == WeaponType.MARK)
                .map(w -> (MarkWeapon) w)
                .findFirst().orElse(null);
        if (markWeapon == null) return;
        if (arrow.getItemStack().getType() != markWeapon.getMaterial()) return;

        if (!markWeapon.isMarkSameClass()) {
            Optional<PlayerState> targetStateOpt = classManager.getStateManager().get(target);
            if (targetStateOpt.isPresent() && targetStateOpt.get().isInClass()
                    && targetStateOpt.get().getCurrentClass().getId().equals(classDef.getId())) {
                WarmupManager.sendMessage(shooter, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().mark().sameClass(), null);
                return;
            }
        }

        PlayerState targetState = classManager.getStateManager().getOrCreate(target);

        int newCount = targetState.getMarkCount() + 1;
        targetState.setMarkCount(newCount);
        targetState.setMarkWeapon(markWeapon);

        MarkLevel level = markWeapon.getLevel(newCount);
        if (level != null) {
            Effects.apply(target, level.getAttackedEffects());
            Effects.apply(shooter, level.getAttackerEffects());
        }

        if (markWeapon.isGiveGlow()) {
            if (level != null && level.isRemoveInvisibility()) {
                target.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            target.setGlowing(true);
        }

        classManager.createMarkBossBar(target, targetState);

        String damage = level != null ? String.valueOf((int) level.getExtraDamage()) : "?";
        WarmupManager.sendMessage(shooter, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().mark().applied(),
                Map.of("target", target.getName(), "damage", damage));
        WarmupManager.sendMessage(target, ClassesMessages.load(ExyliaClasses.getInstance()).weapons().mark().received(),
                Map.of("attacker", shooter.getName(), "damage", damage));
    }

    private Player resolveShooter(Arrow arrow) {
        ProjectileSource source = arrow.getShooter();
        if (source instanceof Player player) return player;
        return null;
    }
}
