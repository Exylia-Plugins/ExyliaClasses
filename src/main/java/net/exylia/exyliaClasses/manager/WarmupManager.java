package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.config.ClassesMessages;
import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Warmup;
import net.exylia.lib.client.Clients;
import net.exylia.lib.client.Cooldown;
import net.exylia.lib.effect.Effects;
import net.exylia.lib.task.TaskHandle;
import net.exylia.lib.task.Tasks;
import net.exylia.lib.text.Text;
import net.exylia.lib.util.TimeFormats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class WarmupManager {

    private static final String WARMUP_COOLDOWN_KEY = "class_warmup";

    private final Map<UUID, TaskHandle> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ClassDefinition> warmupClasses = new ConcurrentHashMap<>();
    private final BiConsumer<Player, ClassDefinition> onComplete;
    private final JavaPlugin plugin;

    public WarmupManager(BiConsumer<Player, ClassDefinition> onComplete,
                         JavaPlugin plugin) {
        this.onComplete = onComplete;
        this.plugin = plugin;
    }

    public void startWarmup(Player player, ClassDefinition classDef) {
        cancelWarmup(player);
        warmupClasses.put(player.getUniqueId(), classDef);

        Warmup warmup = classDef.getWarmup();

        Material chestplate = classDef.getEquipment().get(EquipmentSlot.CHEST);
        Cooldown def = Cooldown.of(WARMUP_COOLDOWN_KEY, Duration.ofSeconds(warmup.getTime()))
                .icon(Cooldown.Icon.item((chestplate != null ? chestplate : Material.COMPASS).name()));
        Clients.cooldowns().show(player, def);

        AtomicInteger remaining = new AtomicInteger(warmup.getTime());

        TaskHandle task = Tasks.of(plugin).runAtEntityTimer(player, 20L, 20L, handle -> {
            if (!player.isOnline()) {
                warmupTasks.remove(player.getUniqueId());
                warmupClasses.remove(player.getUniqueId());
                return;
            }

            int timeLeft = remaining.get();

            if (timeLeft > 0) {
                sendMessage(player, ClassesMessages.load(plugin).warmup().timer(),
                        Map.of("class", classDef.getDisplayName(), "time", String.valueOf(timeLeft),
                                "duration", TimeFormats.render(timeLeft, TimeFormats.Style.COMPACT)));

                if (warmup.getTickSound() != null)
                    playSound(player, warmup.getTickSound());
                if (warmup.getTickParticle() != null)
                    showParticle(player, warmup.getTickParticle());

                remaining.decrementAndGet();
            } else {
                TaskHandle t2 = warmupTasks.remove(player.getUniqueId());
                warmupClasses.remove(player.getUniqueId());
                if (t2 != null) t2.cancel();
                onComplete.accept(player, classDef);
            }
        });

        warmupTasks.put(player.getUniqueId(), task);
    }

    public void cancelWarmup(Player player) {
        TaskHandle task = warmupTasks.remove(player.getUniqueId());
        ClassDefinition classDef = warmupClasses.remove(player.getUniqueId());

        if (task != null) {
            task.cancel();

            Clients.cooldowns().remove(player, WARMUP_COOLDOWN_KEY);

            if (classDef != null && player.isOnline()) {
                Warmup warmup = classDef.getWarmup();
                if (warmup.getCancelSound() != null)
                    playSound(player, warmup.getCancelSound());
                if (warmup.getCancelParticle() != null)
                    showParticle(player, warmup.getCancelParticle());

                sendMessage(player, ClassesMessages.load(plugin).warmup().canceled(), null);
            }
        }
    }

    public boolean isInWarmup(Player player) {
        return warmupTasks.containsKey(player.getUniqueId());
    }

    public ClassDefinition getWarmupClass(Player player) {
        return warmupClasses.get(player.getUniqueId());
    }

    public void cancelAll() {
        warmupTasks.values().forEach(TaskHandle::cancel);
        warmupTasks.clear();
        warmupClasses.clear();
    }

    // --- helpers ---

    public static void sendMessage(Player player, String raw, Map<String, String> ctx) {
        // Text.from, not Text.of: that is what makes %prefix% this plugin's.
        Text text = Text.from(ExyliaClasses.getInstance().getPlugin(), raw).forPlayer(player);

        if (ctx != null) {
            // Formatted, because these values are display names from the class
            // files — "<#c8c8c8><bold>ARCHER</bold>" is its colours, and literal
            // insertion prints the tags. Nothing here is typed by a player;
            // that kind of value belongs in with(), which stays literal.
            for (Map.Entry<String, String> entry : ctx.entrySet()) {
                text = text.withFormatted("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        text.send(player);
    }

    static void playSound(Player player, String definition) {
        if (definition == null || definition.isBlank()) return;
        Effects.soundFrom(definition).show(player);
    }

    static void showParticle(Player player, String definition) {
        if (definition == null || definition.isBlank()) return;
        Effects.particleFrom(definition).at(player.getLocation()).show(player);
    }
}
