package net.exylia.exyliaClasses.menu;

import net.exylia.exyliaClasses.config.ClassesConfig;
import net.exylia.lib.action.ActionResult;
import net.exylia.lib.action.Actions;
import net.exylia.lib.action.PluginActions;
import net.exylia.lib.config.ConfigEdits;
import net.exylia.lib.config.Languages;
import net.exylia.lib.debug.Debug;
import net.exylia.lib.ui.Menus;
import net.exylia.lib.ui.PluginMenus;
import net.exylia.lib.ui.UiKeys;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings an admin really tunes, written by hand: a hub with one card per bar, each
 * opening a page of that bar's settings. Everything else (language, debug, the class files)
 * stays in its file; the class files have their own web editor.
 *
 * <p>A page's values are {@code %<page>_<key>%} (the YAML key, dots and dashes as underscores)
 * and its cards run {@code exyliaclasses:setting <page> <key>}.
 */
public final class SettingsMenus {

    public static final String PERMISSION = "exyliaclasses.admin.settings";
    private static final String HUB = "menus/admin/settings";

    /** A page and the two keys its hub card shows. */
    private static final Map<String, List<String>> PAGES = new LinkedHashMap<>();

    static {
        PAGES.put("energy", List.of("energy-bar.enabled", "energy-bar.colour"));
        PAGES.put("mark", List.of("mark-bar.enabled", "mark-bar.colour"));
    }

    private final Plugin plugin;
    private final Debug debug;
    private final PluginMenus menus;

    public SettingsMenus(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
        this.menus = Menus.of(plugin);
    }

    /** Compiles the screens; the folder is rewritten from the jar on every start. */
    public void load() {
        Languages.replace(plugin, SettingsMenus.class, "menus/admin");
        List<String> ids = new java.util.ArrayList<>(List.of(HUB));
        PAGES.keySet().forEach(id -> ids.add(HUB + "_" + id));
        for (String id : ids) {
            File file = Languages.file(plugin, id + ".yml");
            try {
                menus.load(id, YamlConfiguration.loadConfiguration(file));
            } catch (RuntimeException broken) {
                debug.error("The " + id + " menu could not be read. Fix " + file.getPath() + " and reload.", broken);
            }
        }
    }

    public void registerActions() {
        PluginActions actions = Actions.of(plugin);
        actions.registerSync("settings_hub", PERMISSION, (ctx, args) -> {
            openHub(ctx.player());
            return ActionResult.success();
        });
        actions.registerSync("settings_page", PERMISSION, (ctx, args) -> {
            openPage(ctx.player(), args.string(0, ""));
            return ActionResult.success();
        });
        actions.registerSync("setting", PERMISSION, (ctx, args) -> {
            Player admin = ctx.player();
            String id = args.string(0, "");
            if (PAGES.containsKey(id)) {
                edits().card(admin, args.string(1, ""), ctx.get(UiKeys.CLICK).orElse(null), () -> openPage(admin, id));
            }
            return ActionResult.success();
        });
    }

    /** The hub: only the values its cards show, since every live value is read each second. */
    public void openHub(Player admin) {
        Map<String, Object> context = new LinkedHashMap<>();
        PAGES.forEach((id, shown) -> context.putAll(edits().placeholders(id + "_", shown)));
        menus.open(admin, HUB, context);
    }

    private void openPage(Player admin, String id) {
        if (!PAGES.containsKey(id)) return;
        menus.open(admin, HUB + "_" + id, new LinkedHashMap<>(edits().placeholders(id + "_")));
    }

    private ConfigEdits edits() {
        return ConfigEdits.of(plugin, () -> ClassesConfig.file(plugin));
    }
}
