package gecko10000.GeckoQuests;

import gecko10000.GeckoQuests.misc.Config;
import gecko10000.GeckoQuests.misc.SQLManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.config.ConfigManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class GeckoQuests extends JavaPlugin {

    private static transient GeckoQuests instance;
    private ConfigManager config;
    private ConfigManager questConfig;

    private static Map<UUID, Quest> quests = new LinkedHashMap<>();

    public void onEnable() {
        instance = this;
        //new CommandParser(getResource("command.rdcml"))
        //        .parse().register(getName(), null/*command class*/);
        reload();
        new SQLManager();
    }

    public void onDisable() {
        SQLManager.shutdown();
    }

    public void reload() {
        config = ConfigManager.create(this)
                .target(Config.class).saveDefaults().load();
        questConfig = ConfigManager.create(this, "quests.yml")
                .addConverter(UUID.class, UUID::fromString, UUID::toString)
                .target(GeckoQuests.class).saveDefaults().load();
        quests.values().forEach(Quest::updateParentOfChildren);
    }

    public void saveQuests() {
        questConfig.save();
    }

    public Map<UUID, Quest> getQuests() {
        return quests;
    }

    public static GeckoQuests getInstance() {
        return instance;
    }

    public static String makeReadable(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

}
