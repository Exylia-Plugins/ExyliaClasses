package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.config.GlobalDefaults;
import net.exylia.exyliaClasses.loader.ClassDefinitionLoader;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.Warmup;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.exyliaClasses.util.EquipmentUtil;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.BossBarAPI;
import net.exylia.commons.v2.visual.api.EffectAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClassManager {

    private static final String KEY_ENERGY = "classes:energy";
    private static final String KEY_MARK = "classes:mark";

    private final JavaPlugin plugin;
    private final PlayerStateManager stateManager;
    private final CooldownManager cooldownManager;
    private final WarmupManager warmupManager;
    private final ClassDefinitionLoader loader;

    private final Map<String, ClassDefinition> classes = new HashMap<>();

    public ClassManager(JavaPlugin plugin, PlayerStateManager stateManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.cooldownManager = cooldownManager;
        this.warmupManager = new WarmupManager(this::activateClass);
        this.loader = new ClassDefinitionLoader(plugin);
    }

    public void loadClasses() {
        classes.clear();
        loader.loadAll().forEach(def -> classes.put(def.getId(), def));
    }

    public Map<String, ClassDefinition> getClasses() {
        return Collections.unmodifiableMap(classes);
    }

    public Optional<ClassDefinition> getClass(String id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Optional<ClassDefinition> findClassByEquipment(Player player) {
        for (ClassDefinition def : classes.values()) {
            if (EquipmentUtil.hasRequiredEquipment(player, def)) return Optional.of(def);
        }
        return Optional.empty();
    }

    public void checkArmorChange(Player player) {
        Optional<PlayerState> stateOpt = stateManager.get(player);

        if (warmupManager.isInWarmup(player)) {
            ClassDefinition warmupClass = warmupManager.getWarmupClass(player);
            if (warmupClass != null && !EquipmentUtil.hasRequiredEquipment(player, warmupClass)) {
                warmupManager.cancelWarmup(player);
                findClassByEquipment(player).ifPresent(def -> tryApplyClass(player, def));
            }
            return;
        }

        if (stateOpt.isPresent() && stateOpt.get().isInClass()) {
            ClassDefinition current = stateOpt.get().getCurrentClass();
            if (!EquipmentUtil.hasRequiredEquipment(player, current)) {
                removeClass(player);
                findClassByEquipment(player).ifPresent(def -> tryApplyClass(player, def));
            }
        } else {
            findClassByEquipment(player).ifPresent(def -> tryApplyClass(player, def));
        }
    }

    public void tryApplyClass(Player player, ClassDefinition classDef) {
        if (classDef.hasPermission() && !player.hasPermission(classDef.getPermission())) {
            MessageAPI.send(player, Messages.get("no-permission",
                    "%class%", classDef.getDisplayName()));
            return;
        }
        warmupManager.startWarmup(player, classDef);
    }

    public void activateClass(Player player, ClassDefinition classDef) {
        PlayerState state = stateManager.getOrCreate(player);
        state.setCurrentClass(classDef);

        EffectAPI.applyAllInfinite(player, classDef.getPassiveEffects());

        Energy energy = classDef.getEnergy();
        if (energy != null) {
            state.setEnergy(0);
            updateEnergyBossBar(player, state, energy);
            startEnergyTask(player, state, energy);
        }

        Warmup warmup = classDef.getWarmup();
        if (warmup.getCompletedSound() != null)
            SoundAPI.play(player, warmup.getCompletedSound());
        if (warmup.getCompletedParticle() != null)
            ParticleAPI.spawn(player.getLocation(), ParticleBuilder.fromString(warmup.getCompletedParticle()));

        MessageAPI.send(player, Messages.get("warmup.ready", "%class%", classDef.getDisplayName()));
    }

    public void removeClass(Player player) {
        Optional<PlayerState> stateOpt = stateManager.get(player);
        if (stateOpt.isEmpty()) return;

        PlayerState state = stateOpt.get();
        ClassDefinition classDef = state.getCurrentClass();
        if (classDef == null) return;

        EffectAPI.removeAll(player, classDef.getPassiveEffects());

        cancelEnergyTask(player, state);
        clearMark(player, state);

        state.setCurrentClass(null);
        cooldownManager.clearCooldowns(player);

        if (player.isOnline()) {
            MessageAPI.send(player, Messages.get("class.removed", "%class%", classDef.getDisplayName()));
        }
    }

    public void removeAll(Player player) {
        warmupManager.cancelWarmup(player);
        removeClass(player);
        stateManager.remove(player);
    }

    public void disableAll() {
        warmupManager.cancelAll();
        for (Map.Entry<UUID, PlayerState> entry : stateManager.getAll().entrySet()) {
            PlayerState state = entry.getValue();
            Player player = state.getPlayer();
            ClassDefinition classDef = state.getCurrentClass();
            if (classDef != null) {
                EffectAPI.removeAll(player, classDef.getPassiveEffects());
            }
            cancelEnergyTask(player, state);
            clearMark(player, state);
        }
    }

    private void startEnergyTask(Player player, PlayerState state, Energy energy) {
        ScheduledTask task = TaskAPI.atTimer(player, () -> {
            if (!player.isOnline() || !state.isInClass()) return;
            double current = state.getEnergy();
            double newEnergy = Math.min(current + energy.getRegenPerTick(), energy.getMax());
            if (newEnergy != current) {
                state.setEnergy(newEnergy);
                updateEnergyBossBar(player, state, energy);
            }
        }, 0L, 1L);
        state.setEnergyTask(task);
    }

    private void updateEnergyBossBar(Player player, PlayerState state, Energy energy) {
        if (!GlobalDefaults.BossBars.Energy.ENABLED) return;
        PlaceholderContext ctx = PlaceholderContext.create()
                .put("energy_color", energy.getColor())
                .put("symbol", energy.getSymbol())
                .put("energy", String.format("%.1f", state.getEnergy()))
                .put("max", String.valueOf((int) energy.getMax()))
                .put("progress", state.getEnergy() / energy.getMax());
        BossBarAPI.sendUpdatable(player, KEY_ENERGY, GlobalDefaults.BossBars.Energy.CONFIG, ctx);
    }

    public void createMarkBossBar(Player target, PlayerState state) {
        if (!GlobalDefaults.BossBars.Mark.ENABLED) return;
        MarkLevel level = state.getMarkWeapon().getLevel(state.getMarkCount());
        PlaceholderContext ctx = buildMarkContext(level);
        BossBarAPI.cancel(target, KEY_MARK);
        BossBarAPI.countdownMillisBuilder(target, (long) state.getMarkWeapon().getMarkDuration() * 1000L, GlobalDefaults.BossBars.Mark.CONFIG)
                .key(KEY_MARK)
                .context(ctx)
                .onComplete(() -> expireMark(target, state))
                .start();
    }

    private PlaceholderContext buildMarkContext(MarkLevel level) {
        PlaceholderContext ctx = PlaceholderContext.create();
        if (level != null) {
            ctx.put("mark_color", level.getColor())
               .put("mark_icon", level.getIcon())
               .put("extra_damage", String.valueOf((int) level.getExtraDamage()));
        }
        return ctx;
    }

    private void expireMark(Player player, PlayerState state) {
        if (state.getMarkCount() > 0) {
            player.setGlowing(false);
            state.setMarkCount(0);
            state.setMarkWeapon(null);
        }
        if (player.isOnline()) {
            MessageAPI.send(player, Messages.get("weapons.mark.expired"));
        }
    }

    public void clearMark(Player player, PlayerState state) {
        BossBarAPI.cancel(player, KEY_MARK);
        if (state.getMarkCount() > 0) {
            if (player.isOnline()) {
                player.setGlowing(false);
            }
            state.setMarkCount(0);
            state.setMarkWeapon(null);
        }
    }

    private void cancelEnergyTask(Player player, PlayerState state) {
        if (state.getEnergyTask() != null) {
            state.getEnergyTask().cancel();
            state.setEnergyTask(null);
        }
        BossBarAPI.cancel(player, KEY_ENERGY);
        state.setEnergy(0);
    }

    public PlayerStateManager getStateManager() {
        return stateManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public WarmupManager getWarmupManager() {
        return warmupManager;
    }

    public Collection<ClassDefinition> getAllClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }
}
