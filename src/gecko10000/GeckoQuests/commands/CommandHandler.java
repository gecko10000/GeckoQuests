package gecko10000.GeckoQuests.commands;

import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.guis.QuestEditor;
import gecko10000.GeckoQuests.misc.SQLManager;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.misc.FormatUtils;

public class CommandHandler {

    @CommandHook("modify-progress")
    public void modifyProgress(CommandSender sender, Quest quest, QuestProgressAction action, Player target, long amount) {
        switch (action) {
            case add -> SQLManager.addProgress(quest, target.getUniqueId(), amount)
                    .thenAccept(v -> sender.sendMessage(FormatUtils.color(
                            "&aAdded " + amount + " to " + target.getName() + "'s progress on " + quest.getName() + "."
                    )));
            case set -> SQLManager.setProgress(quest, target.getUniqueId(), amount)
                    .thenAccept(v -> sender.sendMessage(FormatUtils.color(
                            "&aSet " + target.getName() + "'s progress to " + amount + " on " + quest.getName() + "."
                    )));
            case take -> SQLManager.addProgress(quest, target.getUniqueId(), -amount)
                    .thenAccept(v -> sender.sendMessage(FormatUtils.color(
                            "&aTook " + amount + " from " + target.getName() + "'s progress on " + quest.getName() + "."
                    )));
        }
    }

    @CommandHook("edit-all")
    public void editAll(Player player) {
        GeckoQuests.get().getMainEditor().getGui().open(player);
    }

    @CommandHook("edit-quest")
    public void editQuest(Player player, Quest quest) {
        new QuestEditor(quest).open(player);
    }

    @CommandHook("reload")
    public void reload(CommandSender sender) {
        GeckoQuests.get().reload();
        sender.sendMessage(FormatUtils.color("&aConfigs reloaded!"));
    }

}
