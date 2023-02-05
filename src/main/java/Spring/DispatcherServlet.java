package Spring;

import DepInj.Container;
import Spring.Anotations.GetMapping;
import Spring.Anotations.PostMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

public class DispatcherServlet extends HttpServlet {
    Container container = Container.getContainer();

    public static Method getRequestMappingMethod(HttpServletRequest request, Object instance) {
        Method[] methods = instance.getClass().getDeclaredMethods();
        String requestMethod = request.getMethod();
        String requestPath = request.getPathInfo();
        int lastSlashIndex = requestPath.lastIndexOf("/");
        String lastPartOfRequestPath = requestPath.substring(lastSlashIndex);

        for (Method method : methods) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                if (requestMethod.equals("GET") && getMapping.value().equals(lastPartOfRequestPath))
                    return method;
            }
            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if (requestMethod.equals("POST") && postMapping.value().equals(lastPartOfRequestPath))
                    return method;
            }
        }

        return null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getPathInfo();
        int slashIndex = servletPath.lastIndexOf("/");
        String controllerMapping = servletPath.substring(0, slashIndex);

        try {
            Object instance = container.getInstance(controllerMapping);
            if (instance == null)
                throw new RuntimeException("Controller not found");

            Method method = getRequestMappingMethod(req, instance);
            if (method == null)
                throw new RuntimeException("Method not found");

            String methodName = method.getName();
            instance.getClass().getMethod(methodName).invoke(instance);
        } catch (Exception e) {
            throw new RuntimeException("There was an error trying to route the request: " + servletPath, e);
        }
    }
}
