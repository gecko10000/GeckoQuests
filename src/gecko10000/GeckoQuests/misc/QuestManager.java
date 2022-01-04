package gecko10000.GeckoQuests.misc;

import eu.endercentral.crazy_advancements.packet.AdvancementsPacket;
import gecko10000.GeckoQuests.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import redempt.redlib.misc.EventListener;
import redempt.redlib.misc.Task;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
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
            new AdvancementsPacket(player, true, null, null).send();
            additionOrder().values()
                    .forEach(s -> new AdvancementsPacket(player, false, s.stream()
                            .map(Quest::getAdvancement)
                            .collect(Collectors.toList()), null).send());
        });
    }

    private static CompletableFuture<Void> updateProgress(Player player) {
        quests.values().stream()
                .filter(Quest::isRoot)
                .map(Quest::getAdvancement)
                .forEach(a -> a.getProgress(player).setCriteriaProgress(1));
        return SQLManager.getAllProgress(player.getUniqueId())
                .thenAccept(m -> m.forEach((q, p) -> {
                    q.getAdvancement().getProgress(player)
                            .setCriteriaProgress(Utils.completionPercentage(q, p));
                }));
    }

    public static void addQuest(Quest quest) {
        quests.put(quest.getUUID(), quest);
        updateAdvancements();
    }

    public static void removeQuest(Quest quest) {
        quests.remove(quest.getUUID());
        updateAdvancements();
    }

    private void loadingEvents() {
        new EventListener<>(PlayerJoinEvent.class, evt -> addPlayer(evt.getPlayer()));
    }

    private void addPlayer(Player player) {
        Task.syncDelayed(() -> updateAdvancements(player), 2);
    }

}
