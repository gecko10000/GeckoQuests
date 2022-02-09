package gecko10000.GeckoQuests.commands;

import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.misc.Quest;
import gecko10000.GeckoQuests.misc.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.misc.FormatUtils;
import redempt.redlib.misc.UserCache;

public class CommandHandler {

    public CommandHandler() {
        new CommandParser(GeckoQuests.get().getResource("command.rdcml"))
                .setArgTypes(ArgType.of("action", QuestProgressAction.class),
                        new ArgType<>("quest", s -> QuestManager.quests().values().stream()
                                .filter(q -> q.getName().equalsIgnoreCase(s)).findFirst().orElse(null))
                                .tabStream(sender -> QuestManager.quests().values().stream().map(Quest::getName)),
                        new ArgType<>("offlineplayer", UserCache::getOfflinePlayer)
                                .tabStream(s -> Bukkit.getOnlinePlayers().stream().map(Player::getName)))
                .parse().register(GeckoQuests.get().getName(), this);
    }

    @CommandHook("modify-progress")
    public void modifyProgress(CommandSender sender, Quest quest, QuestProgressAction action, OfflinePlayer target, long amount) {
        switch (action) {
            case add -> QuestManager.addProgress(quest, target.getUniqueId(), amount)
                    .thenAccept(t -> sender.sendMessage(FormatUtils.color(
                            "&aAdded " + amount + " (" + t + ") to " + target.getName() + "'s progress on " + quest.getName() + "."
                    )));
            case set -> QuestManager.setProgress(quest, target.getUniqueId(), amount)
                    .thenAccept(v -> sender.sendMessage(FormatUtils.color(
                            "&aSet " + target.getName() + "'s progress to " + amount + " on " + quest.getName() + "."
                    )));
            case take -> QuestManager.addProgress(quest, target.getUniqueId(), -amount)
                    .thenAccept(t -> sender.sendMessage(FormatUtils.color(
                            "&aTook " + amount + " (" + t + ") from " + target.getName() + "'s progress on " + quest.getName() + "."
                    )));
        }
    }

    @CommandHook("edit-all")
    public void editAll(Player player) {
        //GeckoQuests.get().getMainEditor().getGui().open(player);
    }

    @CommandHook("edit-quest")
    public void editQuest(Player player, Quest quest) {
        //new QuestEditor(quest).open(player);
    }

    @CommandHook("reload")
    public void reload(CommandSender sender) {
        GeckoQuests.get().reload();
        QuestManager.updateAdvancements();
        sender.sendMessage(FormatUtils.color("&aConfigs reloaded!"));
    }

}
