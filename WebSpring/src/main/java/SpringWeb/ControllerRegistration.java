package SpringWeb;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControllerRegistration {
    protected Object instance;
    protected boolean isRest = false;
    protected String path = "";
    protected String name = "";
    protected Map<String, Method> getMappings = new HashMap<>();
    protected Map<String, Method> postMappings = new HashMap<>();
    protected Map<String, Method> putMappings = new HashMap<>();
    protected Map<String, Method> deleteMappings = new HashMap<>();

    private static boolean match(String input, String pattern) {
        pattern = pattern.replaceAll("#\\{\\w+}", "\\\\w+");
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        return m.matches();
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public boolean isRest() {
        return isRest;
    }

    public Object getInstance() {
        return instance;
    }

    public Method getGetMethod(String path) {
        return getMethod(path, getMappings);
    }

    public Method getPostMethod(String path) {
        return getMethod(path, postMappings);
    }

    public Method getPutMethod(String path) {
        return getMethod(path, putMappings);
    }

    public Method getDeleteMethod(String path) {
        return getMethod(path, deleteMappings);
    }

    private Method getMethod(String path, Map<String, Method> map) {
        for (String key : map.keySet()) {
            if (match(path, key)) {
                return map.get(key);
            }
        }
        return null;
    }
}
