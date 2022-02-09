package gecko10000.GeckoQuests.misc;

import gecko10000.GeckoQuests.GeckoQuests;
import org.bukkit.event.player.PlayerQuitEvent;
import redempt.redlib.misc.EventListener;
import redempt.redlib.sql.SQLHelper;
import redempt.redlib.sql.SQLHelper.Results;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static SQLHelper sql;

    public SQLManager() {
        if (sql != null) {
            return;
        }
        startDatabase();
    }

    private void startDatabase() {
        Connection connection = Config.mySQL
                ? SQLHelper.openMySQL(Config.ip, Config.port, Config.username, Config.password, Config.database)
                : SQLHelper.openSQLite(GeckoQuests.get().getDataFolder().toPath().resolve("database.db"));
        sql = new SQLHelper(connection);
        sql.execute("""
                CREATE TABLE IF NOT EXISTS quest_progress (
                quest_uuid VARCHAR(36),
                player_uuid VARCHAR(36),
                progress BIGINT,
                PRIMARY KEY (quest_uuid, player_uuid)
                );""");
        sql.setCommitInterval(20*60*5);
        new EventListener<>(PlayerQuitEvent.class, e -> CompletableFuture.runAsync(sql::commit, EXECUTOR));
    }

    protected static CompletableFuture<Map<Quest, Long>> getAllProgress(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Quest, Long> progress = new HashMap<>();
            Results results = sql.queryResults("SELECT quest_uuid, progress FROM quest_progress WHERE player_uuid=?", playerUUID.toString());
            results.forEach(r -> Optional.ofNullable(
                    QuestManager.quests().get(UUID.fromString(r.getString(1))))
                    .ifPresent(q -> progress.put(q, r.getLong(2))));
            results.close();
            QuestManager.quests().forEach((u, q) -> progress.putIfAbsent(q, 0L));
            return progress;
        }, EXECUTOR);
    }

    protected static CompletableFuture<Long> getProgress(Quest quest, UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> getProgressNow(quest, playerUUID), EXECUTOR);
    }

    // internal, should only be called async
    private static long getProgressNow(Quest quest, UUID playerUUID) {
        Long progress = sql.querySingleResultLong(
                "SELECT progress FROM quest_progress WHERE quest_uuid=? and player_uuid=?",
                quest.getUUID().toString(), playerUUID.toString());
        return progress == null ? 0 : progress;
    }

    protected static CompletableFuture<Void> setProgress(Quest quest, UUID playerUUID, Long progress) {
        return CompletableFuture.runAsync(() -> setProgressNow(quest, playerUUID, progress), EXECUTOR);
    }

    // internal
    private static void setProgressNow(Quest quest, UUID playerUUID, Long progress) {
        if (progress == null || progress <= 0) {
            sql.execute("DELETE FROM quest_progress WHERE quest_uuid=? and player_uuid=?;",
                    quest.getUUID().toString(), playerUUID.toString());
        } else {
            sql.execute("REPLACE INTO quest_progress (quest_uuid, player_uuid, progress) VALUES (?, ?, ?);",
                    quest.getUUID().toString(), playerUUID.toString(), progress);
        }
    }

    // returns the new total progress
    protected static CompletableFuture<Long> addProgress(Quest quest, UUID playerUUID, long progress) {
        return CompletableFuture.supplyAsync(() -> {
            long newProgress = getProgressNow(quest, playerUUID) + progress;
            newProgress = Math.max(newProgress, 0);
            setProgressNow(quest, playerUUID, newProgress);
            return newProgress;
        }, EXECUTOR);
    }

    protected static CompletableFuture<Boolean> isDone(Quest quest, UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> getProgressNow(quest, playerUUID) >= quest.getAmount(), EXECUTOR);
    }

    public static void shutdown() {
        sql.commit();
        sql.close();
        EXECUTOR.shutdown();
    }

}
