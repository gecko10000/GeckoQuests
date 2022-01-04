package gecko10000.GeckoQuests;

import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import gecko10000.GeckoQuests.commands.CommandHandler;
import gecko10000.GeckoQuests.commands.QuestProgressAction;
import gecko10000.GeckoQuests.guis.MainEditor;
import gecko10000.GeckoQuests.misc.Config;
import gecko10000.GeckoQuests.misc.QuestManager;
import gecko10000.GeckoQuests.misc.SQLManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.config.ConfigManager;

import java.util.UUID;

public class GeckoQuests extends JavaPlugin {

    private static GeckoQuests instance;
    private ConfigManager config;
    private ConfigManager questConfig;
    private MainEditor mainEditor;

    public void onEnable() {
        instance = this;
        reload(false);
        new SQLManager();
        new QuestManager();
        new CommandParser(getResource("command.rdcml"))
                .setArgTypes(ArgType.of("action", QuestProgressAction.class),
                        new ArgType<>("quest", s -> QuestManager.quests().values().stream().filter(q -> q.getName().equalsIgnoreCase(s)).findFirst().orElse(null))
                                .tabStream(sender -> QuestManager.quests().values().stream().map(Quest::getName)))
                .parse().register(getName(), new CommandHandler());
    }

    public void onDisable() {
        SQLManager.shutdown();
    }

    public void reload(boolean sendAdvancements) {
        Messages.load(this);
        config = ConfigManager.create(this)
                .target(Config.class).saveDefaults().load();
        questConfig = ConfigManager.create(this, "quests.yml")
                .addConverter(UUID.class, UUID::fromString, UUID::toString)
                .addConverter(AdvancementVisibility.class, AdvancementVisibility::parseVisibility, AdvancementVisibility::getName)
                .target(QuestManager.class).saveDefaults().load();
        QuestManager.quests().values().forEach(Quest::updateParentOfChildren);
        if (sendAdvancements) {
            QuestManager.updateAdvancements();
        }
        mainEditor = new MainEditor();
    }

    public void saveQuests() {
        questConfig.save();
        QuestManager.updateAdvancements();
    }

    public static GeckoQuests get() {
        return instance;
    }

    public MainEditor getMainEditor() {
        return mainEditor;
    }

}
