import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final Map<String, String> config = new HashMap<>();

    public static void setConfig(String key, String value){
        config.put(key, value);
    }

    public static String getConfig(String key){
        return config.getOrDefault(key, null);
    }
}
