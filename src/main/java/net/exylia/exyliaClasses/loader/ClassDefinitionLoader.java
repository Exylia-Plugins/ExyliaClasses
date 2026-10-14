package net.exylia.exyliaClasses.loader;

import net.exylia.exyliaClasses.ExyliaClasses;
import net.exylia.exyliaClasses.model.*;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassive;
import net.exylia.exyliaClasses.model.specialpassive.SpecialPassiveType;
import net.exylia.exyliaClasses.model.weapon.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import net.exylia.lib.config.BundledFiles;
import net.exylia.lib.debug.Debug;

public class ClassDefinitionLoader {

    private final JavaPlugin plugin;
    private final Debug debug;

    public ClassDefinitionLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.debug = Debug.of(plugin);
    }

    public List<ClassDefinition> loadAll() {
        // Refreshed on every load so classes and keys added in a release reach
        // existing servers; the *.yml filter below skips the .yml.v1 backups.
        BundledFiles.refresh(plugin, ExyliaClasses.class, "classes");

        File classesDir = new File(plugin.getDataFolder(), "classes");
        List<ClassDefinition> result = new ArrayList<>();

        File[] files = classesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return result;

        for (File file : files) {
            String name = file.getName().replace(".yml", "");
            try {
                ClassDefinition def = load(name);
                if (def != null) result.add(def);
            } catch (Exception e) {
                debug.error("Error loading class from " + file.getName() + ": " + e.getMessage(), e);
            }
        }
        return result;
    }

    private ClassDefinition load(String name) {
        File file = new File(plugin.getDataFolder(), "classes/" + name + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
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
            debug.warn("Invalid material for " + key + ": " + value);
        }
    }

    private Warmup parseWarmup(ConfigurationSection section) {
        if (section == null) return new Warmup(5, null, null, null, null, null, null);
        int time = section.getInt("time", 5);
        return new Warmup(time,
            parseSoundString(section, "sound"),
            parseParticleString(section, "particle"),
            parseSoundString(section, "sound-completed"),
            parseParticleString(section, "particle-completed"),
            parseSoundString(section, "sound-cancel"),
            parseParticleString(section, "particle-cancel"));
    }

    private String parseSoundString(ConfigurationSection parent, String key) {
        ConfigurationSection s = parent.getConfigurationSection(key);
        if (s == null || !s.getBoolean("enabled", false)) return null;
        return s.getString("type");
    }

    private String parseParticleString(ConfigurationSection parent, String key) {
        ConfigurationSection s = parent.getConfigurationSection(key);
        if (s == null || !s.getBoolean("enabled", false)) return null;
        return s.getString("type");
    }

    private Energy parseEnergy(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) return null;
        return new Energy(
            section.getString("name", "Energy"),
            section.getString("symbol", "⚡"),
            section.getString("color", "#ffffff"),
            section.getDouble("max", 100),
            section.getDouble("regen-per-tick", 0.1));
    }

    private Map<Material, Ability> parseAbilities(ConfigurationSection section) {
        Map<Material, Ability> map = new EnumMap<>(Material.class);
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try {
                Material m = Material.valueOf(key.toUpperCase());
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) continue;
                map.put(m, new Ability(
                    s.getString("display-name", key),
                    m,
                    AbilityType.fromString(s.getString("type", "SELF")),
                    s.getStringList("effects"),
                    s.getInt("cooldown", 30),
                    s.getDouble("energy-cost", 0),
                    s.getDouble("radius", 0),
                    parseParticleString(s, "particles"),
                    parseSoundString(s, "sound")));
            } catch (IllegalArgumentException e) {
                debug.warn("Invalid material for ability: " + key);
            }
        }
        return map;
    }

    private Map<Material, HoldEffect> parseHoldEffects(ConfigurationSection section) {
        Map<Material, HoldEffect> map = new EnumMap<>(Material.class);
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try {
                Material m = Material.valueOf(key.toUpperCase());
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) continue;
                map.put(m, new HoldEffect(m,
                    AbilityType.fromString(s.getString("type", "SELF")),
                    s.getDouble("radius", 0),
                    s.getStringList("effects")));
            } catch (IllegalArgumentException e) {
                debug.warn("Invalid material for hold-effect: " + key);
            }
        }
        return map;
    }

    private Map<Material, Weapon> parseWeapons(ConfigurationSection section) {
        Map<Material, Weapon> map = new EnumMap<>(Material.class);
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try {
                Material m = Material.valueOf(key.toUpperCase());
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) continue;
                WeaponType type = WeaponType.valueOf(s.getString("type", "MARK").toUpperCase());
                Weapon weapon = switch (type) {
                    case MARK -> parseMarkWeapon(m, s);
                    case BACKSTAB -> parseBackstabWeapon(m, s);
                };
                if (weapon != null) map.put(m, weapon);
            } catch (IllegalArgumentException e) {
                debug.warn("Invalid material or type for weapon: " + key);
            }
        }
        return map;
    }

    private MarkWeapon parseMarkWeapon(Material material, ConfigurationSection section) {
        Map<Integer, MarkLevel> levels = new HashMap<>();
        ConfigurationSection marksSection = section.getConfigurationSection("marks");
        if (marksSection != null) {
            for (String levelKey : marksSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection lvl = marksSection.getConfigurationSection(levelKey);
                    if (lvl == null) continue;
                    levels.put(level, new MarkLevel(level,
                        lvl.getDouble("extra-damage", 0),
                        lvl.getString("color", "&f"),
                        lvl.getString("icon", "◆"),
                        lvl.getBoolean("remove-invisibility", false),
                        lvl.getStringList("attacked-effects"),
                        lvl.getStringList("attacker-effects"),
                        null, null, null, null));
                } catch (NumberFormatException e) {
                    debug.warn("Invalid mark level: " + levelKey);
                }
            }
        }
        return new MarkWeapon(material,
            section.getInt("mark-duration", 10),
            section.getBoolean("give-glow", false),
            section.getBoolean("mark-same-class", false),
            levels);
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
                debug.warn("Invalid special passive type: " + typeStr);
                continue;
            }
            List<String> effects = effectsObj instanceof List<?> list
                ? list.stream().map(Object::toString).toList() : List.of();
            result.add(new SpecialPassive(type, condition, effects));
        }
        return result;
    }

    private BackstabWeapon parseBackstabWeapon(Material material, ConfigurationSection section) {
        return new BackstabWeapon(material,
            section.getDouble("extra-damage", 50),
            section.getBoolean("can-dead", false),
            section.getDouble("attack-cooldown-need", 0.9),
            section.getBoolean("break-item", false),
            section.getStringList("attacked-effects"),
            section.getStringList("attacker-effects"),
            parseParticleString(section, "attacked-particles"),
            parseParticleString(section, "attacker-particles"),
            parseSoundString(section, "attacked-sound"),
            parseSoundString(section, "attacker-sound"));
    }
}
