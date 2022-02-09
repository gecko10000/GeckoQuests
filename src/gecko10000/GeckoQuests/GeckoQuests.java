package gecko10000.GeckoQuests;

import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import gecko10000.GeckoQuests.commands.CommandHandler;
import gecko10000.GeckoQuests.misc.Config;
import gecko10000.GeckoQuests.misc.Quest;
import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.misc.SQLManager;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.misc.UserCache;

import java.util.UUID;

public class GeckoQuests extends JavaPlugin {

    private static GeckoQuests instance;
    private ConfigManager config;
    private ConfigManager questConfig;

    public void onEnable() {
        instance = this;
        UserCache.asyncInit();
        reload();
        new SQLManager();
        new QuestManager();
        new CommandHandler();


    }

    public void onDisable() {
        SQLManager.shutdown();
    }

    public void reload() {
        Messages.load(this);
        config = ConfigManager.create(this)
                .target(Config.class).saveDefaults().load();
        questConfig = ConfigManager.create(this, "quests.yml")
                .addConverter(UUID.class, UUID::fromString, UUID::toString)
                .addConverter(AdvancementVisibility.class, AdvancementVisibility::parseVisibility, AdvancementVisibility::getName)
                .target(QuestManager.class).saveDefaults().load();
        QuestManager.quests().values().forEach(Quest::updateParentOfChildren);
    }

    public void saveQuests() {
        questConfig.save();
        QuestManager.updateAdvancements();
    }

    public static GeckoQuests get() {
        return instance;
    }

}
