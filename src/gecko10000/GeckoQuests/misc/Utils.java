package gecko10000.GeckoQuests.misc;

import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    public static int completionPercentage(Quest quest, long progress) {
        long amount = quest.getAmount();
        if (progress >= amount) {
            return 100;
        }
        int percentage = Math.round(100* (progress / (float) amount));
        return Math.min(percentage, 99);
    }

}
