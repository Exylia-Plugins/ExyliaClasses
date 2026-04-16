package net.exylia.exyliaClasses.loader;

import net.exylia.exyliaClasses.model.*;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassiveType;
import net.exylia.exyliaClasses.model.weapon.*;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class ClassDefinitionLoader {

    private final JavaPlugin plugin;

    public ClassDefinitionLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public List<ClassDefinition> loadAll() {
        Configs.get("classes/Archer");
        Configs.get("classes/Bard");
        Configs.get("classes/Miner");
        Configs.get("classes/Rogue");
        Configs.get("classes/Sorcerer");
        Configs.get("classes/Tank");

        File classesDir = new File(plugin.getDataFolder(), "classes");
        List<ClassDefinition> result = new ArrayList<>();

        File[] files = classesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return result;

        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            try {
                ClassDefinition def = load(name);
                if (def != null) {
                    result.add(def);
                }
            } catch (Exception e) {
                DebugAPI.logPluginError("Error al cargar clase desde " + file.getName(), e);
            }
        }
        return result;
    }

    private ClassDefinition load(String name) {
        FileConfiguration cfg = Configs.get("classes/" + name).raw();
        String id = name.toLowerCase();
        String displayName = cfg.getString("display-name", id);
        String permission = cfg.getString("permission", "NONE");

        Map<EquipmentSlot, Material> equipment = parseEquipment(cfg.getConfigurationSection("equipment"));
        List<String> passiveEffects = cfg.getStringList("passive-effects");
        List<SpecialPassive> specialPassives = parseSpecialPassives(cfg.getMapList("special-passives"));
        Warmup warmup = parseWarmup(cfg.getConfigurationSection("warmup"));
        Energy energy = parseEnergy(cfg.getConfigurationSection("energy"));
        Map<Material, Ability> abilities = parseAbilities(cfg.getConfigurationSection("abilities"));
        Map<Material, HoldEffect> holdEffects = parseHoldEffects(cfg.getConfigurationSection("hold-effects"));
        Map<Material, Weapon> weapons = parseWeapons(cfg.getConfigurationSection("weapons"));

        return new ClassDefinition(id, displayName, permission, equipment,
                passiveEffects, specialPassives, warmup, energy, abilities, holdEffects, weapons);
    }

    private Map<EquipmentSlot, Material> parseEquipment(ConfigurationSection section) {
        Map<EquipmentSlot, Material> map = new EnumMap<>(EquipmentSlot.class);
        if (section == null) return map;

        parseEquipmentSlot(section, "helmet", EquipmentSlot.HEAD, map);
        parseEquipmentSlot(section, "chestplate", EquipmentSlot.CHEST, map);
        parseEquipmentSlot(section, "leggings", EquipmentSlot.LEGS, map);
        parseEquipmentSlot(section, "boots", EquipmentSlot.FEET, map);
        return map;
    }

    private void parseEquipmentSlot(ConfigurationSection section, String key, EquipmentSlot slot,
                                     Map<EquipmentSlot, Material> map) {
        String value = section.getString(key);
        if (value == null) return;
        try {
            map.put(slot, Material.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            DebugAPI.logPluginWarn("Material inválido para " + key + ": " + value);
        }
    }

    private Warmup parseWarmup(ConfigurationSection section) {
        if (section == null) return new Warmup(5, null, null, null, null, null, null);

        int time = section.getInt("time", 5);
        String tickSound = parseSoundSection(section.getConfigurationSection("sound"));
        String tickParticle = parseParticleSection(section.getConfigurationSection("particle"));
        String completedSound = parseSoundSection(section.getConfigurationSection("sound-completed"));
        String completedParticle = parseParticleSection(section.getConfigurationSection("particle-completed"));
        String cancelSound = parseSoundSection(section.getConfigurationSection("sound-cancel"));
        String cancelParticle = parseParticleSection(section.getConfigurationSection("particle-cancel"));

        return new Warmup(time, tickSound, tickParticle, completedSound, completedParticle, cancelSound, cancelParticle);
    }

    private String parseSoundSection(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) return null;
        return section.getString("type");
    }

    private String parseParticleSection(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) return null;
        return section.getString("type");
    }

    private Energy parseEnergy(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) return null;
        String name = section.getString("name", "Energy");
        String symbol = section.getString("symbol", "⚡");
        String color = section.getString("color", "#ffffff");
        double max = section.getDouble("max", 100);
        double regen = section.getDouble("regen-per-tick", 0.1);
        return new Energy(name, symbol, color, max, regen);
    }

    private Map<Material, Ability> parseAbilities(ConfigurationSection section) {
        Map<Material, Ability> map = new EnumMap<>(Material.class);
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                ConfigurationSection abilitySection = section.getConfigurationSection(key);
                if (abilitySection == null) continue;

                String displayName = abilitySection.getString("display-name", key);
                AbilityType type = AbilityType.fromString(abilitySection.getString("type", "SELF"));
                List<String> effects = abilitySection.getStringList("effects");
                int cooldown = abilitySection.getInt("cooldown", 30);
                double energyCost = abilitySection.getDouble("energy-cost", 0);
                double radius = abilitySection.getDouble("radius", 0);
                String particles = parseParticleSection(abilitySection.getConfigurationSection("particles"));
                String sound = parseSoundSection(abilitySection.getConfigurationSection("sound"));

                map.put(material, new Ability(displayName, material, type, effects, cooldown, energyCost, radius, particles, sound));
            } catch (IllegalArgumentException e) {
                DebugAPI.logPluginWarn("Material inválido para habilidad: " + key);
            }
        }
        return map;
    }

    private Map<Material, HoldEffect> parseHoldEffects(ConfigurationSection section) {
        Map<Material, HoldEffect> map = new EnumMap<>(Material.class);
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                ConfigurationSection holdSection = section.getConfigurationSection(key);
                if (holdSection == null) continue;

                AbilityType type = AbilityType.fromString(holdSection.getString("type", "SELF"));
                double radius = holdSection.getDouble("radius", 0);
                List<String> effects = holdSection.getStringList("effects");

                map.put(material, new HoldEffect(material, type, radius, effects));
            } catch (IllegalArgumentException e) {
                DebugAPI.logPluginWarn("Material inválido para hold-effect: " + key);
            }
        }
        return map;
    }

    private Map<Material, Weapon> parseWeapons(ConfigurationSection section) {
        Map<Material, Weapon> map = new EnumMap<>(Material.class);
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                ConfigurationSection weaponSection = section.getConfigurationSection(key);
                if (weaponSection == null) continue;

                WeaponType type = WeaponType.valueOf(weaponSection.getString("type", "MARK").toUpperCase());
                Weapon weapon = switch (type) {
                    case MARK -> parseMarkWeapon(material, weaponSection);
                    case BACKSTAB -> parseBackstabWeapon(material, weaponSection);
                };
                if (weapon != null) map.put(material, weapon);
            } catch (IllegalArgumentException e) {
                DebugAPI.logPluginWarn("Material o tipo inválido para arma: " + key);
            }
        }
        return map;
    }

    private MarkWeapon parseMarkWeapon(Material material, ConfigurationSection section) {
        int duration = section.getInt("mark-duration", 10);
        boolean giveGlow = section.getBoolean("give-glow", false);
        boolean markSameClass = section.getBoolean("mark-same-class", false);

        Map<Integer, MarkLevel> levels = new HashMap<>();
        ConfigurationSection marksSection = section.getConfigurationSection("marks");
        if (marksSection != null) {
            for (String levelKey : marksSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection lvlSection = marksSection.getConfigurationSection(levelKey);
                    if (lvlSection == null) continue;

                    double extraDamage = lvlSection.getDouble("extra-damage", 0);
                    String color = lvlSection.getString("color", "&f");
                    String icon = lvlSection.getString("icon", "◆");
                    boolean removeInvis = lvlSection.getBoolean("remove-invisibility", false);
                    List<String> attackedEffects = lvlSection.getStringList("attacked-effects");
                    List<String> attackerEffects = lvlSection.getStringList("attacker-effects");

                    levels.put(level, new MarkLevel(level, extraDamage, color, icon, removeInvis,
                            attackedEffects, attackerEffects, null, null, null, null));
                } catch (NumberFormatException e) {
                    DebugAPI.logPluginWarn("Nivel de marca inválido: " + levelKey);
                }
            }
        }
        return new MarkWeapon(material, duration, giveGlow, markSameClass, levels);
    }

    @SuppressWarnings("unchecked")
    private List<SpecialPassive> parseSpecialPassives(List<Map<?, ?>> mapList) {
        List<SpecialPassive> result = new ArrayList<>();
        if (mapList == null || mapList.isEmpty()) return result;

        for (Map<?, ?> entry : mapList) {
            String typeStr = (String) entry.get("type");
            String condition = String.valueOf(entry.get("condition"));
            Object effectsObj = entry.get("effects");

            SpecialPassiveType type = SpecialPassiveType.fromString(typeStr);
            if (type == null) {
                DebugAPI.logPluginWarn("Tipo de pasiva especial inválido: " + typeStr);
                continue;
            }

            List<String> effects = effectsObj instanceof List<?> list
                    ? list.stream().map(Object::toString).toList()
                    : List.of();

            result.add(new SpecialPassive(type, condition, effects));
        }
        return result;
    }

    private BackstabWeapon parseBackstabWeapon(Material material, ConfigurationSection section) {
        double extraDamage = section.getDouble("extra-damage", 50);
        boolean canDead = section.getBoolean("can-dead", false);
        double cooldownNeed = section.getDouble("attack-cooldown-need", 0.9);
        boolean breakItem = section.getBoolean("break-item", false);
        List<String> attackedEffects = section.getStringList("attacked-effects");
        List<String> attackerEffects = section.getStringList("attacker-effects");
        String attackedParticles = parseParticleSection(section.getConfigurationSection("attacked-particles"));
        String attackerParticles = parseParticleSection(section.getConfigurationSection("attacker-particles"));
        String attackedSound = parseSoundSection(section.getConfigurationSection("attacked-sound"));
        String attackerSound = parseSoundSection(section.getConfigurationSection("attacker-sound"));

        return new BackstabWeapon(material, extraDamage, canDead, cooldownNeed, breakItem,
                attackedEffects, attackerEffects, attackedParticles, attackerParticles, attackedSound, attackerSound);
    }
}
