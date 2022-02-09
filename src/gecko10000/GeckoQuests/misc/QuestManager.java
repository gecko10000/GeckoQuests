package gecko10000.GeckoQuests.misc;

import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.advancement.progress.AdvancementProgress;
import eu.endercentral.crazy_advancements.packet.AdvancementsPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import redempt.redlib.misc.EventListener;
import redempt.redlib.misc.Task;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QuestManager {

    private static Map<UUID, Quest> quests = new LinkedHashMap<>();

    public QuestManager() {
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
        loadingEvents();
    }

    public static Map<UUID, Quest> quests() {
        return quests;
    }

    public static CompletableFuture<Void> setProgress(Quest quest, UUID uuid, long progress) {
        updateProgress(quest, uuid, progress);
        return SQLManager.setProgress(quest, uuid, progress);
    }

    // returns the new total
    public static CompletableFuture<Long> addProgress(Quest quest, UUID uuid, long progress) {
        return SQLManager.addProgress(quest, uuid, progress)
                .thenApply(p -> {
                    updateProgress(quest, uuid, p);
                    return p;
                });
    }

    private static void updateProgress(Quest quest, UUID uuid, long progress) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        Advancement advancement = quest.getAdvancement();
        advancement.getProgress(uuid).setCriteriaProgress(quest.isRoot() ? 1 : Utils.completionPercentage(quest, progress));
        new AdvancementsPacket(player, false, List.of(advancement), null).send();
    }

    private static Map<Quest, LinkedHashSet<Quest>> additionOrder() {
        Map<Quest, LinkedHashSet<Quest>> ordered = new LinkedHashMap<>();
        quests.values().stream()
                .filter(Quest::isRoot)
                .forEach(root -> {
                    LinkedHashSet<Quest> children = new LinkedHashSet<>();
                    root.getAllChildren(children);
                    ordered.put(root, children);
                });
        return ordered;
    }

    public static void updateAdvancements() {
        Bukkit.getOnlinePlayers().forEach(QuestManager::updateAdvancements);
    }

    /*
    Retrieve progress from the database,
    remove existing advancements, set new progress, and send it to the player
    (the progress must be set before the info is sent to the player, duh)
     */
    private static void updateAdvancements(Player player) {
        updateProgress(player).thenAccept(v -> {
            new AdvancementsPacket(player, true, quests.values().stream().map(Quest::getAdvancement).collect(Collectors.toList()), null).send();
            /*additionOrder().values()
                    .forEach(s -> new AdvancementsPacket(player, false, s.stream()
                            .map(Quest::getAdvancement)
                            .collect(Collectors.toList()), null).send());*/
        });
    }

    public static CompletableFuture<Void> updateProgress(Player player) {
        return SQLManager.getAllProgress(player.getUniqueId())
                .thenAccept(m -> m.forEach((q, p) -> q.getAdvancement().getProgress(player)
                            .setCriteriaProgress(Utils.completionPercentage(q, p))));
    }

    public static CompletableFuture<Void> updateProgress(Quest quest, Player player) {
        AdvancementProgress progress = quest.getAdvancement().getProgress(player);
        if (quest.isRoot()) {
            progress.setCriteriaProgress(1);
            return CompletableFuture.completedFuture(null);
        }
        return SQLManager.getProgress(quest, player.getUniqueId())
                .thenAccept(l -> progress.setCriteriaProgress(Utils.completionPercentage(quest, l)));
    }

    public static void addQuest(Quest quest) {
        quests.put(quest.getUUID(), quest);
    }

    public static void removeQuest(Quest quest) {
        quests.remove(quest.getUUID());
    }

    private void loadingEvents() {
        new EventListener<>(PlayerJoinEvent.class, evt -> addPlayer(evt.getPlayer()));
    }

    private void addPlayer(Player player) {
        Task.syncDelayed(() -> updateAdvancements(player), 2);
    }

}
