package net.exylia.exyliaClasses.manager;

import net.exylia.exyliaClasses.model.ClassDefinition;
import net.exylia.exyliaClasses.model.Warmup;
import net.exylia.exyliaClasses.config.MessagesDefaults;
import net.exylia.commons.v2.clientapi.cooldown.api.CooldownAPI;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownIcon;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.builder.ParticleBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class WarmupManager {

    private final Map<UUID, ScheduledTask> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ClassDefinition> warmupClasses = new ConcurrentHashMap<>();
    private final BiConsumer<Player, ClassDefinition> onComplete;

    public WarmupManager(BiConsumer<Player, ClassDefinition> onComplete) {
        this.onComplete = onComplete;
    }

    private static final String WARMUP_COOLDOWN_KEY = "class_warmup";

    public void startWarmup(Player player, ClassDefinition classDef) {
        cancelWarmup(player);
        warmupClasses.put(player.getUniqueId(), classDef);

        Warmup warmup = classDef.getWarmup();

        Material chestplate = classDef.getEquipment().get(EquipmentSlot.CHEST);
        CooldownDefinition cooldownDef = CooldownDefinition.builder()
                .name(WARMUP_COOLDOWN_KEY)
                .duration(Duration.ofSeconds(warmup.getTime()))
                .icon(chestplate != null ? CooldownIcon.item(chestplate.name()) : CooldownIcon.item("COMPASS"))
                .build();
        CooldownAPI.display(player, cooldownDef);

        AtomicInteger remaining = new AtomicInteger(warmup.getTime());

        ScheduledTask task = TaskAPI.atTimer(player, () -> {
            if (!player.isOnline()) {
                warmupTasks.remove(player.getUniqueId());
                warmupClasses.remove(player.getUniqueId());
                return;
            }

            int timeLeft = remaining.get();

            if (timeLeft > 0) {
                MessageAPI.send(player, MessagesDefaults.Warmup.TIMER, PlaceholderContext.create()
                        .put("class", classDef.getDisplayName())
                        .put("time", timeLeft));

                if (warmup.getTickSound() != null)
                    SoundAPI.play(player, warmup.getTickSound());
                if (warmup.getTickParticle() != null)
                    ParticleAPI.spawn(player.getLocation(), ParticleBuilder.fromString(warmup.getTickParticle()));

                remaining.decrementAndGet();
            } else {
                ScheduledTask t = warmupTasks.remove(player.getUniqueId());
                warmupClasses.remove(player.getUniqueId());
                if (t != null) t.cancel();
                onComplete.accept(player, classDef);
            }
        }, 20L, 20L);

        warmupTasks.put(player.getUniqueId(), task);
    }

    public void cancelWarmup(Player player) {
        ScheduledTask task = warmupTasks.remove(player.getUniqueId());
        ClassDefinition classDef = warmupClasses.remove(player.getUniqueId());

        if (task != null) {
            task.cancel();

            CooldownAPI.remove(player, WARMUP_COOLDOWN_KEY);

            if (classDef != null && player.isOnline()) {
                Warmup warmup = classDef.getWarmup();
                if (warmup.getCancelSound() != null)
                    SoundAPI.play(player, warmup.getCancelSound());
                if (warmup.getCancelParticle() != null)
                    ParticleAPI.spawn(player.getLocation(), ParticleBuilder.fromString(warmup.getCancelParticle()));
                MessageAPI.send(player, MessagesDefaults.Warmup.CANCELED);
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
        warmupTasks.values().forEach(ScheduledTask::cancel);
        warmupTasks.clear();
        warmupClasses.clear();
    }
}
