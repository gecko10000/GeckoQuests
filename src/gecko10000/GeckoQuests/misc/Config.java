package gecko10000.GeckoQuests.misc;

import redempt.redlib.config.annotations.ConfigName;

public class Config {

    @ConfigName ("my-sql.use")
    public static boolean mySQL = false;

    @ConfigName ("my-sql.ip")
    public static String ip = "192.168.1.1";

    @ConfigName ("my-sql.port")
    public static int port = 3306;

    @ConfigName ("my-sql.username")
    public static String username = "root";

    @ConfigName ("my-sql.password")
    public static String password = "password";

    @ConfigName ("my-sql.database")
    public static String database = "GeckoQuests";

}
