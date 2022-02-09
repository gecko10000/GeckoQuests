package gecko10000.GeckoQuests.misc;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    public static int completionPercentage(Quest quest, long progress) {
        if (quest.isRoot()) {
            return 1;
        }
        long amount = quest.getAmount();
        if (progress >= amount) {
            return 100;
        }
        int percentage = (int) Math.round(100.0 * progress / amount);
        return Math.min(percentage, 99);
    }

}
