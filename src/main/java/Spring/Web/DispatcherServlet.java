package Spring.Web;

import Spring.*;
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
    private final static ApplicationContext context = SpringApplication.applicationContext;
    private final static Gson gson = new Gson();
    private static ControllerManager conManager;

    @Override
    public void init() throws ServletException {
        BeanCreator beanCreator = context.getBeanCreator();
        try {
            ControllerManager instance = beanCreator.getInstance(ControllerManager.class);
            context.registerBean(ControllerManager.class.getName(), instance);
            conManager = (ControllerManager) context.getBean(ControllerManager.class);
        } catch (Exception e) {
            throw new ServletException("Could not load the ControllerManager",e);
        }
    }

    //TODO: Add @PathVariable support. It should be able to get the path variables from the request and pass them to the method.
    //TODO: Add @RequestBody support. It should be able to get the body of the request and pass it to the method.

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

        ControllerRegistration registration = conManager.getController(controllerMapping);
        if (registration == null) {
            springResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found", req);
            return;
        }

        Method method = switch (req.getMethod()) {
            case "GET" -> registration.getGetMethod(methodPath);
            case "POST" -> registration.getPostMethod(methodPath);
            case "PUT" -> registration.getPutMethod(methodPath);
            case "DELETE" -> registration.getDeleteMethod(methodPath);
            default -> null;
        };
        if (method == null) {
            springResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed", req);
            return;
        }

        Object resultFromMethod;
        try {
            resultFromMethod = method.invoke(registration.getInstance());
        } catch (Exception e) {
            springResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", req);
            e.printStackTrace();
            return;
        }


        if (registration.isRest()) {
            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(resultFromMethod));
            return;
        }
        resp.getWriter().write(gson.toJson(resultFromMethod));
    }
}
