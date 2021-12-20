package gecko10000.GeckoQuests;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import gecko10000.GeckoQuests.misc.Config;
import gecko10000.GeckoQuests.misc.SQLManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigValue;

import java.util.Map;
import java.util.UUID;

public class GeckoQuests extends JavaPlugin {

    private static GeckoQuests instance;
    private ConfigManager config;
    private ConfigManager questConfig;

    @ConfigValue
    private Map<UUID, Quest> quests = ConfigManager.map(UUID.class, Quest.class);

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
        config = new ConfigManager(this)
                .register(Config.class).saveDefaults().load();
        questConfig = new ConfigManager(this, "quests.yml")
                .addConverter(UUID.class, UUID::fromString, UUID::toString)
                .register(this).saveDefaults().load();
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

}
