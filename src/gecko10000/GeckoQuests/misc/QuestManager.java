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

    private static LinkedHashSet<Quest> additionOrder() {
        LinkedHashSet<Quest> ordered = new LinkedHashSet<>();
        quests.values().stream()
                .filter(Quest::isRoot)
                .forEach(root -> root.getAllChildren(ordered));
        return ordered;
    }

    public static void update() {
        Bukkit.getOnlinePlayers().forEach(QuestManager::update);
    }

    private static void update(Player player) {
        new AdvancementsPacket(player, true,
                additionOrder().stream()
                        .map(Quest::getAdvancement)
                        .collect(Collectors.toList()), null).send();
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
        Task.syncDelayed(() -> {
            update(player);
        }, 2);
    }

}
