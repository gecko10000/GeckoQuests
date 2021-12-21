package gecko10000.GeckoQuests.misc;

import gecko10000.GeckoQuests.GeckoQuests;
import gecko10000.GeckoQuests.objects.Quest;
import redempt.redlib.sql.SQLCache;
import redempt.redlib.sql.SQLHelper;

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static SQLHelper sql;
    private static SQLCache cache;

    public SQLManager() {
        if (sql != null) {
            return;
        }
        startDatabase();
    }

    private void startDatabase() {
        Connection connection = Config.mySQL
                ? SQLHelper.openMySQL(Config.ip, Config.port, Config.username, Config.password, Config.database)
                : SQLHelper.openSQLite(GeckoQuests.getInstance().getDataFolder().toPath().resolve("database.db"));
        sql = new SQLHelper(connection);
        execute("CREATE TABLE IF NOT EXISTS quest_progress (" +
                "quest_uuid VARCHAR(36)," +
                "player_uuid VARCHAR(36)," +
                "progress BIGINT," +
                "PRIMARY KEY (quest_uuid, player_uuid)" +
                "CHECK (progress > 0)" +
                ");")
                .thenAccept(v -> cache = sql.createCache("quest_progress", "progress", "quest_uuid", "player_uuid"));
        sql.setCommitInterval(20*60*5);
    }

    public static CompletableFuture<Long> getProgress(Quest quest, UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long progress = sql.querySingleResultLong(
                        "SELECT progress FROM quest_progress WHERE quest_uuid=? and player_uuid=?",
                        quest.getUUID(), playerUUID);
                return progress == null ? 0 : progress;
            } catch (Exception e) {
                e.printStackTrace();
                return 0L;
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<Void> setProgress(Quest quest, UUID playerUUID, Long progress) {
        return progress == null || progress <= 0 ? execute(
                "DELETE FROM quest_progress WHERE quest_uuid=? and player_uuid=?;",
                quest.getUUID(), playerUUID) : execute(
                "REPLACE INTO quest_progress (quest_uuid, player_uuid, progress) VALUES (?, ?, ?);",
                quest.getUUID(), playerUUID, progress);
    }

    public static CompletableFuture<Void> addProgress(Quest quest, UUID playerUUID, long progress) {
        try {
            return setProgress(quest, playerUUID, getProgress(quest, playerUUID).get() + progress);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Boolean> isDone(Quest quest, UUID playerUUID) {
        return getProgress(quest, playerUUID).thenApply(p -> p >= quest.getAmount());
    }

    private static CompletableFuture<Void> execute(String statement, Object... args) {
        return CompletableFuture.runAsync(() -> {
            try {
                sql.execute(statement, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, EXECUTOR);
    }

    public static void shutdown() {
        sql.commit();
        sql.close();
        EXECUTOR.shutdown();
    }

}
