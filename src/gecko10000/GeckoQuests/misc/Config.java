package gecko10000.GeckoQuests.misc;

import redempt.redlib.configmanager.annotations.ConfigValue;

public class Config {

    @ConfigValue ("my-sql.use")
    public static boolean mySQL = false;

    @ConfigValue ("my-sql.ip")
    public static String ip = "192.168.1.1";

    @ConfigValue ("my-sql.port")
    public static int port = 3306;

    @ConfigValue ("my-sql.username")
    public static String username = "root";

    @ConfigValue ("my-sql.password")
    public static String password = "password";

    @ConfigValue ("my-sql.database")
    public static String database = "GeckoQuests";

}
