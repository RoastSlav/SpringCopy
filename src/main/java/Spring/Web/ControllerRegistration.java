package Spring.Web;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ControllerRegistration {
    protected Object instance;
    protected boolean isRest = false;
    protected String path = "";
    protected String name = "";
    protected Map<String, Method> getMappings = new HashMap<>();
    protected Map<String, Method> postMappings = new HashMap<>();
    protected Map<String, Method> putMappings = new HashMap<>();
    protected Map<String, Method> deleteMappings = new HashMap<>();

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
        return getMappings.get(path);
    }

    public Method getPostMethod(String path) {
        return postMappings.get(path);
    }

    public Method getPutMethod(String path) {
        return putMappings.get(path);
    }

    public Method getDeleteMethod(String path) {
        return deleteMappings.get(path);
    }
}
