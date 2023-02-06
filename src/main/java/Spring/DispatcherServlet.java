package Spring;

import DepInj.Container;
import Spring.Anotations.DeleteMapping;
import Spring.Anotations.GetMapping;
import Spring.Anotations.PostMapping;
import Spring.Anotations.PutMapping;
import com.google.gson.Gson;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DispatcherServlet extends HttpServlet implements Servlet {
    private final static Container container = Container.getContainer();
    private final static Gson gson = new Gson();

    private static Method getRequestMappingMethod(String methodPath, String requestMethod, Object instance) {
        Method[] methods = instance.getClass().getDeclaredMethods();

        for (Method method : methods) {
            switch (requestMethod) {
                case "GET" -> {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping getMapping = method.getAnnotation(GetMapping.class);
                        if (getMapping.value().equals(methodPath))
                            return method;
                    }
                }
                case "POST" -> {
                    if (method.isAnnotationPresent(PostMapping.class)) {
                        PostMapping postMapping = method.getAnnotation(PostMapping.class);
                        if (postMapping.value().equals(methodPath))
                            return method;
                    }
                }
                case "PUT" -> {
                    if (method.isAnnotationPresent(PutMapping.class)) {
                        PutMapping putMapping = method.getAnnotation(PutMapping.class);
                        if (putMapping.value().equals(methodPath))
                            return method;
                    }
                }
                case "DELETE" -> {
                    if (method.isAnnotationPresent(DeleteMapping.class)) {
                        DeleteMapping putMapping = method.getAnnotation(DeleteMapping.class);
                        if (putMapping.value().equals(methodPath))
                            return method;
                    }
                }
            }
        }

        return null;
    }

    private List<String> splitPath(String path) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                result.add(path.substring(0, i));
                path = path.substring(i);
                i = 0;
            }
        }
        result.add(path);
        return result;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpServletSpringResponse springResponse = (HttpServletSpringResponse) resp;
        List<String> splitPath = splitPath(req.getPathInfo());

        String controllerMapping = splitPath.get(0);
        String methodPath = splitPath.size() == 1 ? "" : splitPath.get(1);

        Object instance;
        try {
            instance = container.getInstance(controllerMapping);
        } catch (Exception e) {
            springResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found", req);
            return;
        }

        Method method = getRequestMappingMethod(methodPath, req.getMethod(), instance);
        if (method == null) {
            springResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", req);
            return;
        }

        String methodName = method.getName();
        Object resultFromMethod;
        try {
            resultFromMethod = instance.getClass().getMethod(methodName).invoke(instance);
        } catch (Exception e) {
            springResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", req);
            return;
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(resultFromMethod));
    }
}
