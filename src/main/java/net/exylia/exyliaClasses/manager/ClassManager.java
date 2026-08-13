package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.loader.ClassDefinitionLoader;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.Warmup;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.exyliaClasses.util.EquipmentUtil;
import net.exylia.lib.clan.Clans;
import net.exylia.lib.effect.Display;
import net.exylia.lib.effect.Effects;
import net.exylia.lib.placeholder.Placeholders;
import net.exylia.lib.task.TaskHandle;
import net.exylia.lib.task.Tasks;
import net.exylia.lib.util.ItemCooldowns;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ClassManager {

    private static final String KEY_ENERGY = "classes:energy";
    private static final String KEY_MARK = "classes:mark";

    private final JavaPlugin plugin;
    private final PlayerStateManager stateManager;
    private final WarmupManager warmupManager;
    private final ClassDefinitionLoader loader;

    private final Map<String, ClassDefinition> classes = new HashMap<>();

    public ClassManager(JavaPlugin plugin, PlayerStateManager stateManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        ClassesMessages.load((net.exylia.exyliaClasses.ExyliaClasses) plugin);
        this.warmupManager = new WarmupManager(this::activateClass, plugin);
        this.loader = new ClassDefinitionLoader(plugin);
    }

    public void loadClasses() {
        classes.clear();
        loader.loadAll().forEach(def -> classes.put(def.getId(), def));
    }

    public Map<String, ClassDefinition> getClasses() { return Collections.unmodifiableMap(classes); }
    public Optional<ClassDefinition> getClass(String id) { return Optional.ofNullable(classes.get(id)); }

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
            WarmupManager.sendMessage(player, ClassesMessages.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).noPermission(),
                    Map.of("class", classDef.getDisplayName()));
            return;
        }
        warmupManager.startWarmup(player, classDef);
    }

    public void activateClass(Player player, ClassDefinition classDef) {
        PlayerState state = stateManager.getOrCreate(player);
        state.setCurrentClass(classDef);

        net.exylia.lib.util.Effects.applyInfinite(player, classDef.getPassiveEffects());

        Energy energy = classDef.getEnergy();
        if (energy != null) {
            state.setEnergy(0);
            updateEnergyBossBar(player, state, energy);
            startEnergyTask(player, state, energy);
        }

        Warmup warmup = classDef.getWarmup();
        if (warmup.getCompletedSound() != null)
            WarmupManager.playSound(player, warmup.getCompletedSound());
        if (warmup.getCompletedParticle() != null)
            WarmupManager.showParticle(player, warmup.getCompletedParticle());

        WarmupManager.sendMessage(player, ClassesMessages.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).warmup().ready(),
                Map.of("class", classDef.getDisplayName()));
    }

    public void removeClass(Player player) {
        Optional<PlayerState> stateOpt = stateManager.get(player);
        if (stateOpt.isEmpty()) return;

        PlayerState state = stateOpt.get();
        ClassDefinition classDef = state.getCurrentClass();
        if (classDef == null) return;

        net.exylia.lib.util.Effects.remove(player, classDef.getPassiveEffects());

        if (!state.getActiveSpecialPassives().isEmpty()) {
            for (var sp : state.getActiveSpecialPassives()) {
                net.exylia.lib.util.Effects.remove(player, sp.getEffects());
            }
            state.getActiveSpecialPassives().clear();
        }

        cancelEnergyTask(player, state);
        clearMark(player, state);

        for (var material : classDef.getAbilities().keySet()) {
            ItemCooldowns.clear(player, material);
            net.exylia.lib.client.Clients.cooldowns().remove(player,
                    "ability_" + material.name());
        }

        state.setCurrentClass(null);

        if (player.isOnline()) {
            WarmupManager.sendMessage(player, ClassesMessages.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).classSection().removed(),
                    Map.of("class", classDef.getDisplayName()));
        }
    }

    public void removeAll(Player player) {
        warmupManager.cancelWarmup(player);
        removeClass(player);
        stateManager.remove(player);
    }

    public void disableAll() {
        warmupManager.cancelAll();
        for (var entry : stateManager.getAll().entrySet()) {
            PlayerState state = entry.getValue();
            if (state.getPlayer().isOnline()) {
                Player player = state.getPlayer();
                ClassDefinition classDef = state.getCurrentClass();
                if (classDef != null) {
                    net.exylia.lib.util.Effects.remove(player, classDef.getPassiveEffects());
                    if (!state.getActiveSpecialPassives().isEmpty()) {
                        for (var sp : state.getActiveSpecialPassives()) {
                            net.exylia.lib.util.Effects.remove(player, sp.getEffects());
                        }
                        state.getActiveSpecialPassives().clear();
                    }
                }
                cancelEnergyTask(player, state);
                clearMark(player, state);
            }
        }
    }

    // --- energy ---

    private void startEnergyTask(Player player, PlayerState state, Energy energy) {
        TaskHandle task = Tasks.of(plugin).runTimer(0L, 1L, handle -> {
            if (!player.isOnline() || !state.isInClass()) return;
            double current = state.getEnergy();
            double newEnergy = Math.min(current + energy.getRegenPerTick(), energy.getMax());
            if (newEnergy != current) {
                state.setEnergy(newEnergy);
                updateEnergyBossBar(player, state, energy);
            }
        });
        state.setEnergyTask(task);
    }

    private void updateEnergyBossBar(Player player, PlayerState state, Energy energy) {
        ClassesConfig.BossBarConfig bb = ClassesConfig.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).energyBar();
        if (!bb.enabled()) return;

        float progress = (float) (state.getEnergy() / energy.getMax());
        String text = Placeholders.apply(bb.text(), player,
                Map.of("energy_color", (Object) energy.getColor(),
                        "symbol", energy.getSymbol(),
                        "energy", String.format("%.1f", state.getEnergy()),
                        "max", String.valueOf((int) energy.getMax())));

        Display display = state.getEnergyDisplay();
        if (display == null || !display.isShowing()) {
            display = Effects.bossBar(text)
                    .colour(bb.colour())
                    .overlay(bb.overlay())
                    .progress(progress)
                    .every(1L)
                    .show(player);
            state.setEnergyDisplay(display);
        } else {
            display.text(text).progress(progress);
        }
    }

    private void cancelEnergyTask(Player player, PlayerState state) {
        if (state.getEnergyTask() != null) {
            state.getEnergyTask().cancel();
            state.setEnergyTask(null);
        }
        Display display = state.getEnergyDisplay();
        if (display != null) {
            display.stop();
            state.setEnergyDisplay(null);
        }
        state.setEnergy(0);
    }

    // --- mark ---

    public void createMarkBossBar(Player target, PlayerState state) {
        ClassesConfig.BossBarConfig bb = ClassesConfig.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).markBar();
        if (!bb.enabled()) return;

        MarkLevel level = state.getMarkWeapon().getLevel(state.getMarkCount());
        Map<String, Object> ctx = buildMarkContext(level);

        String text = Placeholders.apply(bb.text(), target, ctx);
        Display display = Effects.bossBar(text)
                .colour(bb.colour())
                .overlay(bb.overlay())
                .countdown(state.getMarkWeapon().getMarkDuration())
                .onEnd(() -> expireMark(target, state))
                .show(target);
        // Store for cleanup
        state.setMarkDisplay(display);
    }

    private Map<String, Object> buildMarkContext(MarkLevel level) {
        Map<String, Object> ctx = new HashMap<>();
        if (level != null) {
            ctx.put("mark_color", level.getColor());
            ctx.put("mark_icon", level.getIcon());
            ctx.put("extra_damage", String.valueOf((int) level.getExtraDamage()));
        }
        return ctx;
    }

    private void expireMark(Player player, PlayerState state) {
        if (state.getMarkCount() > 0) {
            player.setGlowing(false);
            state.setMarkCount(0);
            state.setMarkWeapon(null);
        }
        state.setMarkDisplay(null);
        if (player.isOnline()) {
            WarmupManager.sendMessage(player, ClassesMessages.load((net.exylia.exyliaClasses.ExyliaClasses) plugin).weapons().mark().expired(), null);
        }
    }

    public void clearMark(Player player, PlayerState state) {
        Display display = state.getMarkDisplay();
        if (display != null) {
            display.stop();
            state.setMarkDisplay(null);
        }
        if (state.getMarkCount() > 0) {
            if (player.isOnline()) player.setGlowing(false);
            state.setMarkCount(0);
            state.setMarkWeapon(null);
        }
    }

    // --- clan utility ---

    public List<Player> getClanMembers(Player caster, double radius) {
        List<Player> targets = new ArrayList<>();
        targets.add(caster);

        var clanOpt = Clans.clanOf(caster);
        if (clanOpt.isEmpty()) return targets;

        var clan = clanOpt.get();
        double radiusSquared = radius * radius;

        for (UUID memberId : Clans.onlineMembersOf(caster.getUniqueId())) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member == null || !member.isOnline()) continue;
            if (member.equals(caster)) continue;
            if (!caster.getWorld().equals(member.getWorld())) continue;
            if (caster.getLocation().distanceSquared(member.getLocation()) > radiusSquared) continue;
            targets.add(member);
        }
        return targets;
    }

    public List<Player> getEnemies(Player caster, double radius) {
        List<Player> targets = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (Player nearby : caster.getWorld().getPlayers()) {
            if (nearby.equals(caster)) continue;
            if (caster.getLocation().distanceSquared(nearby.getLocation()) > radiusSquared) continue;
            if (Clans.areInSameClan(caster, nearby)) continue;
            targets.add(nearby);
        }
        return targets;
    }

    public List<Player> getTargets(Player caster, String type, double radius) {
        return switch (type.toLowerCase()) {
            case "self" -> List.of(caster);
            case "to_clan", "to_clan_and_ally" -> getClanMembers(caster, radius);
            case "to_ally" -> List.of();
            case "to_enemy" -> getEnemies(caster, radius);
            default -> List.of(caster);
        };
    }

    // --- accessors ---

    public void reapplyPassiveEffects(Player player) {
        stateManager.get(player).ifPresent(state -> {
            if (state.isInClass()) {
                net.exylia.lib.util.Effects.applyInfinite(player,
                        state.getCurrentClass().getPassiveEffects());
                for (var sp : state.getActiveSpecialPassives()) {
                    net.exylia.lib.util.Effects.applyInfinite(player, sp.getEffects());
                }
            }
        });
    }

    public PlayerStateManager getStateManager() { return stateManager; }
    public WarmupManager getWarmupManager() { return warmupManager; }
    public Collection<ClassDefinition> getAllClasses() { return Collections.unmodifiableCollection(classes.values()); }

    // --- configured visual effects ---

    public void showParticle(Player player, String definition) {
        WarmupManager.showParticle(player, definition);
    }

    public void playSound(Player player, String definition) {
        WarmupManager.playSound(player, definition);
    }
}
