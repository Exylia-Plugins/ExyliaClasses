package net.exylia.exyliaClasses.util;

import net.exylia.exyliaClasses.model.ClassDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class EquipmentUtil {

    private EquipmentUtil() {}

    public static boolean hasRequiredEquipment(Player player, ClassDefinition classDef) {
        PlayerInventory inv = player.getInventory();
        for (Map.Entry<EquipmentSlot, Material> entry : classDef.getEquipment().entrySet()) {
            ItemStack item = getArmorPiece(inv, entry.getKey());
            if (item == null || item.getType() != entry.getValue()) return false;
        }
        return true;
    }

    private static ItemStack getArmorPiece(PlayerInventory inv, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> inv.getHelmet();
            case CHEST -> inv.getChestplate();
            case LEGS -> inv.getLeggings();
            case FEET -> inv.getBoots();
            default -> null;
        };
    }
}
