package net.exylia.exyliaClasses.api;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.manager.ClassManager;
import net.exylia.exyliaClasses.model.Ability;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Energy;
import net.exylia.exyliaClasses.model.PlayerState;
import net.exylia.lib.api.classes.AbilityType;
import net.exylia.lib.api.classes.AbilityUseResult;
import net.exylia.lib.api.classes.ClassAbility;
import net.exylia.lib.api.classes.ClassEnergy;
import net.exylia.lib.api.classes.ClassesService;
import net.exylia.lib.api.classes.PlayerClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What third-party plugins see of ExyliaClasses.
 *
 * <p>A translation layer over {@link ClassesAPI} and the class manager, and
 * nothing more. Every method delegates, and the only work done here is turning
 * the plugin's own types into the records the API publishes — because those
 * types live in this plugin's classloader, which no other plugin can reach, and
 * a class definition carries hold effects, special passives and weapons whose
 * shape has to stay free to change.
 *
 * <p>The plugin's own code keeps calling {@link ClassesAPI} and the manager
 * directly. Routing internal calls through a service lookup would mean asking
 * Bukkit for a reference to ourselves, and would freeze every internal
 * signature into a public contract we could no longer change.
 *
 * @see net.exylia.lib.api.classes.ClassesService
 */
public final class ClassesServiceImpl implements ClassesService {

    private ClassesServiceImpl() {
    }

    /**
     * Publishes the service so other plugins can find it.
     *
     * <p>Registered against the plugin, so unregistration happens by itself
     * when it disables and a reload cannot leave a stale implementation behind.
     *
     * @param plugin this plugin
     */
    public static void register(@NotNull Plugin plugin) {
        plugin.getServer().getServicesManager()
                .register(ClassesService.class, new ClassesServiceImpl(), plugin,
                        ServicePriority.Normal);
    }

    // ── The catalogue ──────────────────────────────────────────────────────

    @Override
    @NotNull
    public List<PlayerClass> classes() {
        Collection<ClassDefinition> source = api().getAllClasses();
        List<PlayerClass> out = new ArrayList<>(source.size());
        for (ClassDefinition definition : source) {
            out.add(toClass(definition));
        }
        return List.copyOf(out);
    }

    @Override
    @NotNull
    public Optional<PlayerClass> classById(@NotNull String classId) {
        return api().getClass(classId).map(ClassesServiceImpl::toClass);
    }

    @Override
    public boolean exists(@NotNull String classId) {
        return api().getClass(classId).isPresent();
    }

    @Override
    @NotNull
    public List<ClassAbility> abilities(@NotNull String classId) {
        return api().getClass(classId)
                .map(definition -> {
                    List<ClassAbility> out = new ArrayList<>(definition.getAbilities().size());
                    definition.getAbilities().forEach((trigger, ability) ->
                            out.add(toAbility(trigger, ability)));
                    return List.copyOf(out);
                })
                .orElse(List.of());
    }

    @Override
    @NotNull
    public Optional<ClassEnergy> energy(@NotNull String classId) {
        return api().getClass(classId).map(ClassDefinition::getEnergy).map(ClassesServiceImpl::toEnergy);
    }

    // ── Permissions ────────────────────────────────────────────────────────

    @Override
    public boolean canUse(@NotNull Player player, @NotNull String classId) {
        return api().getClass(classId)
                .map(definition -> !definition.hasPermission()
                        || player.hasPermission(definition.getPermission()))
                .orElse(false);
    }

    // ── What a player is ───────────────────────────────────────────────────

    @Override
    public boolean isInClass(@NotNull UUID player) {
        return state(player).filter(PlayerState::isInClass).isPresent();
    }

    @Override
    @NotNull
    public Optional<PlayerClass> classOf(@NotNull UUID player) {
        return state(player)
                .filter(PlayerState::isInClass)
                .map(PlayerState::getCurrentClass)
                .map(ClassesServiceImpl::toClass);
    }

    @Override
    public double energyOf(@NotNull UUID player) {
        return state(player).map(PlayerState::getEnergy).orElse(0.0);
    }

    @Override
    public int marks(@NotNull UUID player) {
        return state(player).map(PlayerState::getMarkCount).orElse(0);
    }

    @Override
    public boolean isMarked(@NotNull UUID player) {
        return state(player).filter(PlayerState::isMarked).isPresent();
    }

    @Override
    @NotNull
    public Optional<PlayerClass> warmingUp(@NotNull Player player) {
        ClassManager manager = manager();
        if (!manager.getWarmupManager().isInWarmup(player)) return Optional.empty();
        return Optional.ofNullable(manager.getWarmupManager().getWarmupClass(player))
                .map(ClassesServiceImpl::toClass);
    }

    @Override
    @NotNull
    public Optional<PlayerClass> classForEquipment(@NotNull Player player) {
        return manager().findClassByEquipment(player).map(ClassesServiceImpl::toClass);
    }

    // ── Actions ────────────────────────────────────────────────────────────

    @Override
    public void apply(@NotNull Player player, @NotNull String classId) {
        api().getClass(classId).ifPresent(definition -> manager().tryApplyClass(player, definition));
    }

    @Override
    public void remove(@NotNull Player player) {
        api().removeFromClass(player);
    }

    @Override
    @NotNull
    public AbilityUseResult useAbility(@NotNull Player player, @NotNull Material trigger) {
        return switch (manager().useAbility(player, trigger)) {
            case USED -> AbilityUseResult.USED;
            case NOT_IN_CLASS -> AbilityUseResult.NOT_IN_CLASS;
            case UNKNOWN_ABILITY -> AbilityUseResult.UNKNOWN_ABILITY;
            case ON_COOLDOWN -> AbilityUseResult.ON_COOLDOWN;
            case NOT_ENOUGH_ENERGY -> AbilityUseResult.NOT_ENOUGH_ENERGY;
            case CANCELLED -> AbilityUseResult.CANCELLED;
        };
    }

    // ── Energy ─────────────────────────────────────────────────────────────

    @Override
    public boolean giveEnergy(@NotNull Player player, double amount) {
        return manager().giveEnergy(player, amount);
    }

    @Override
    public boolean spendEnergy(@NotNull Player player, double amount) {
        return manager().spendEnergy(player, amount);
    }

    // ── Translation ────────────────────────────────────────────────────────

    private static ClassesAPI api() {
        return ExyliaClasses.getInstance().getClassesAPI();
    }

    private static ClassManager manager() {
        return ExyliaClasses.getInstance().getClassManager();
    }

    private static Optional<PlayerState> state(UUID player) {
        return manager().getStateManager().get(player);
    }

    /**
     * A class as the public API sees it.
     *
     * <p>Public because the events the plugin fires carry the same shape as the
     * service returns, and building it twice is how the two drift apart.
     */
    public static PlayerClass toClass(ClassDefinition definition) {
        return new PlayerClass(definition.getId(), definition.getDisplayName(),
                // "NONE", blank and absent all mean the same thing to the plugin
                // and would each mean something different to a caller reading it.
                definition.hasPermission() ? definition.getPermission() : "",
                Map.copyOf(definition.getEquipment()));
    }

    public static ClassAbility toAbility(org.bukkit.Material trigger, Ability ability) {
        return new ClassAbility(ability.getDisplayName(), trigger, toAbilityType(ability.getType()),
                ability.getCooldown(), ability.getEnergyCost(), ability.getRadius());
    }

    private static ClassEnergy toEnergy(Energy energy) {
        return new ClassEnergy(energy.getName(), energy.getSymbol(), energy.getMax(),
                energy.getRegenPerTick());
    }

    private static AbilityType toAbilityType(net.exylia.exyliaClasses.model.AbilityType type) {
        return switch (type) {
            case SELF -> AbilityType.SELF;
            case TO_CLAN -> AbilityType.TO_CLAN;
            case TO_ALLY -> AbilityType.TO_ALLY;
            case TO_ENEMY -> AbilityType.TO_ENEMY;
            case TO_CLAN_AND_ALLY -> AbilityType.TO_CLAN_AND_ALLY;
        };
    }
}
