package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.api.ClassesServiceImpl;
import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.loader.ClassDefinitionLoader;
import net.exylia.exyliaClasses.model.Ability;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.exyliaClasses.model.Warmup;
import net.exylia.exyliaClasses.model.weapon.MarkLevel;
import net.exylia.exyliaClasses.util.ClanHelper;
import net.exylia.exyliaClasses.util.EquipmentUtil;
import net.exylia.lib.api.classes.event.AbilityUseEvent;
import net.exylia.lib.api.classes.event.ClassEquipEvent;
import net.exylia.lib.api.classes.event.ClassRemoveEvent;
import net.exylia.lib.clan.Clans;
import net.exylia.lib.effect.Display;
import net.exylia.lib.effect.Effects;
import net.exylia.lib.format.Numbers;
import net.exylia.lib.placeholder.Placeholders;
import net.exylia.lib.task.TaskHandle;
import net.exylia.lib.task.Tasks;
import net.exylia.lib.util.ItemCooldowns;
import net.exylia.lib.util.TimeFormats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
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
        ClassesMessages.load(plugin);
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
            WarmupManager.sendMessage(player, ClassesMessages.load(plugin).noPermission(),
                    Map.of("class", classDef.getDisplayName()));
            return;
        }
        warmupManager.startWarmup(player, classDef);
    }

    public void activateClass(Player player, ClassDefinition classDef) {
        if (!new ClassEquipEvent(player, ClassesServiceImpl.toClass(classDef)).callEvent()) return;
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

        WarmupManager.sendMessage(player, ClassesMessages.load(plugin).warmup().ready(),
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

        // Ability cooldowns are deliberately kept: they live per UUID in ExyliaLib and
        // expire on their own, so swapping armour, relogging or dying cannot reset them.

        state.setCurrentClass(null);

        if (player.isOnline()) {
            WarmupManager.sendMessage(player, ClassesMessages.load(plugin).classSection().removed(),
                    Map.of("class", classDef.getDisplayName()));
        }

        new ClassRemoveEvent(player, ClassesServiceImpl.toClass(classDef)).callEvent();
    }

    public void removeAll(Player player) {
        warmupManager.cancelWarmup(player);
        removeClass(player);
        // A marked player without a class still glows: clear the mark regardless.
        stateManager.get(player).ifPresent(state -> clearMark(player, state));
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

    // --- abilities ---

    public enum AbilityOutcome { USED, NOT_IN_CLASS, UNKNOWN_ABILITY, ON_COOLDOWN, NOT_ENOUGH_ENERGY, CANCELLED }

    public AbilityOutcome useAbility(Player player, Material material) {
        Optional<PlayerState> stateOpt = stateManager.get(player);
        if (stateOpt.isEmpty() || !stateOpt.get().isInClass()) return AbilityOutcome.NOT_IN_CLASS;

        PlayerState state = stateOpt.get();
        ClassDefinition classDef = state.getCurrentClass();
        Ability ability = classDef.getAbilities().get(material);
        if (ability == null) return AbilityOutcome.UNKNOWN_ABILITY;

        if (ItemCooldowns.isActive(player, material)) {
            WarmupManager.sendMessage(player, ClassesMessages.load(plugin).abilities().cooldown(),
                    Map.of("ability", ability.getDisplayName(),
                            "time", ItemCooldowns.remainingFormatted(player, material),
                            "duration", TimeFormats.render(ItemCooldowns.remainingSeconds(player, material), TimeFormats.Style.PRECISE)));
            return AbilityOutcome.ON_COOLDOWN;
        }

        double energyCost = ability.getEnergyCost();
        if (energyCost > 0 && state.getEnergy() < energyCost) {
            WarmupManager.sendMessage(player, ClassesMessages.load(plugin).abilities().noEnergy(),
                    Map.of("needed", Numbers.decimals(energyCost, 0),
                            "current", Numbers.decimals(state.getEnergy(), 1)));
            return AbilityOutcome.NOT_ENOUGH_ENERGY;
        }

        if (!new AbilityUseEvent(player, ClassesServiceImpl.toClass(classDef),
                ClassesServiceImpl.toAbility(material, ability)).callEvent()) {
            return AbilityOutcome.CANCELLED;
        }

        if (energyCost > 0) {
            state.setEnergy(state.getEnergy() - energyCost);
        }

        if (ability.getCooldown() > 0) {
            Duration duration = Duration.ofSeconds(ability.getCooldown());
            // ItemCooldowns also puts it on the HUD of Lunar and SurvivalCore.
            ItemCooldowns.start(player, material, duration);
        }

        List<Player> targets = ClanHelper.getTargets(player, ability.getType(), ability.getRadius());
        for (Player target : targets) {
            if (!target.isOnline()) continue;
            net.exylia.lib.util.Effects.apply(target, ability.getEffects());
            if (ability.getParticles() != null) showParticle(target, ability.getParticles());
        }

        if (ability.getSound() != null) playSound(player, ability.getSound());

        WarmupManager.sendMessage(player, ClassesMessages.load(plugin).abilities().used(),
                Map.of("ability", ability.getDisplayName()));

        for (Player target : targets) {
            if (!target.equals(player)) notifyTarget(target, player, ability);
        }
        return AbilityOutcome.USED;
    }

    private void notifyTarget(Player target, Player caster, Ability ability) {
        String message = switch (ability.getType()) {
            case TO_CLAN, TO_CLAN_AND_ALLY, TO_ALLY -> ClassesMessages.load(plugin).abilities().clan();
            case TO_ENEMY -> ClassesMessages.load(plugin).abilities().enemy();
            default -> null;
        };
        if (message == null) return;
        WarmupManager.sendMessage(target, message,
                Map.of("player", caster.getName(), "ability", ability.getDisplayName()));
    }

    // --- energy ---

    public boolean giveEnergy(Player player, double amount) {
        if (amount < 0) throw new IllegalArgumentException("Energy amount cannot be negative: " + amount);
        PlayerState state = energyState(player);
        if (state == null) return false;
        Energy energy = state.getCurrentClass().getEnergy();
        state.setEnergy(Math.min(state.getEnergy() + amount, energy.getMax()));
        updateEnergyBossBar(player, state, energy);
        return true;
    }

    public boolean spendEnergy(Player player, double amount) {
        if (amount < 0) throw new IllegalArgumentException("Energy amount cannot be negative: " + amount);
        PlayerState state = energyState(player);
        if (state == null || state.getEnergy() < amount) return false;
        state.setEnergy(state.getEnergy() - amount);
        updateEnergyBossBar(player, state, state.getCurrentClass().getEnergy());
        return true;
    }

    private PlayerState energyState(Player player) {
        return stateManager.get(player)
                .filter(state -> state.isInClass() && state.getCurrentClass().getEnergy() != null)
                .orElse(null);
    }

    private void startEnergyTask(Player player, PlayerState state, Energy energy) {
        TaskHandle task = Tasks.of(plugin).runAtEntityTimer(player, 0L, 1L, handle -> {
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
        ClassesConfig.BossBarConfig bb = ClassesConfig.load(plugin).energyBar();
        if (!bb.enabled()) return;

        float progress = (float) (state.getEnergy() / energy.getMax());
        String text = Placeholders.apply(ClassesMessages.load(plugin).bossBars().energy(), player,
                Map.of("energy_color", (Object) energy.getColor(),
                        "symbol", energy.getSymbol(),
                        "energy", Numbers.decimals(state.getEnergy(), 1),
                        "max", String.valueOf((int) energy.getMax())));

        Display display = state.getEnergyDisplay();
        if (display == null || !display.isShowing()) {
            display = Effects.of(plugin).bossBar(text)
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
        ClassesConfig.BossBarConfig bb = ClassesConfig.load(plugin).markBar();
        if (!bb.enabled()) return;

        MarkLevel level = state.getMarkWeapon().getLevel(state.getMarkCount());
        Map<String, Object> ctx = buildMarkContext(level);

        // A new hit restarts the countdown: drop the previous bar first, detached so its
        // end does not expire the refreshed mark.
        Display previous = state.getMarkDisplay();
        state.setMarkDisplay(null);
        if (previous != null) previous.stop();

        String text = Placeholders.apply(ClassesMessages.load(plugin).bossBars().mark(), target, ctx);
        Display[] self = new Display[1];
        Display display = Effects.of(plugin).bossBar(text)
                .colour(bb.colour())
                .overlay(bb.overlay())
                .countdown(state.getMarkWeapon().getMarkDuration())
                .onEnd(() -> {
                    if (self[0] != null && state.getMarkDisplay() == self[0]) expireMark(target, state);
                })
                .show(target);
        self[0] = display;
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
            WarmupManager.sendMessage(player, ClassesMessages.load(plugin).weapons().mark().expired(), null);
        }
    }

    public void clearMark(Player player, PlayerState state) {
        Display display = state.getMarkDisplay();
        if (display != null) {
            state.setMarkDisplay(null);
            display.stop();
        }
        if (state.getMarkCount() > 0) {
            // Unconditional: the glowing flag is saved with the player, so it must go even on quit.
            player.setGlowing(false);
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

    /** Redraws the client overlay of ability cooldowns that survived a relog or respawn. */
    public void restoreCooldownOverlays(Player player) {
        for (ClassDefinition def : classes.values()) {
            for (Material material : def.getAbilities().keySet()) ItemCooldowns.restore(player, material);
        }
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
