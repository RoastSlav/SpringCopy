package SpringWeb;

import Spring.Anotations.Initializer;
import Spring.ApplicationContext;
import Spring.BeanCreator;
import Spring.SpringApplication;
import SpringWeb.Annotation.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet implements Servlet, Initializer {
    private final static Pattern pathPattern = Pattern.compile("\\/\\w+");
    private final static Pattern pathVariablePattern = Pattern.compile("#\\{(\\w+)}");
    private final static ApplicationContext context = SpringApplication.applicationContext;
    private final static Gson gson = new GsonBuilder().setLenient().create();
    private static ControllerManager conManager;

    @Override
    public void init() throws ServletException {
        BeanCreator beanCreator = context.getBeanCreator();
        try {
            ControllerManager instance = beanCreator.getInstance(ControllerManager.class);
            context.registerBean(ControllerManager.class.getName(), instance);
            conManager = (ControllerManager) context.getBean(ControllerManager.class);
        } catch (Exception e) {
            throw new ServletException("Could not load the SpringWeb.ControllerManager", e);
        }
    }

    private List<String> splitPath(String path) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pathPattern.matcher(path);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpServletSpringResponse springResponse = (HttpServletSpringResponse) resp;
        String requestURI = req.getRequestURI();

        List<String> path = splitPath(requestURI);
        if (path.isEmpty()) {
            springResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found", req);
            return;
        }

        String controllerMapping = path.remove(0);
        String methodPath = path.remove(0) + String.join("/", path);

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

        String pathAnnotationValue = getAnnotationValue(method, req.getMethod());
        Object[] methodParamValues = getMethodParamValues(method, req, pathAnnotationValue, path);
        Object resultFromMethod;
        try {
            resultFromMethod = method.invoke(registration.getInstance(), methodParamValues);
        } catch (Exception e) {
            springResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", req);
            e.printStackTrace();
            return;
        }


        if (registration.isRest() || method.isAnnotationPresent(ResponseBody.class)) {
            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(resultFromMethod));
            return;
        }
        resp.getWriter().write(resultFromMethod.toString());
    }

    private Object[] getMethodParamValues(Method method, HttpServletRequest req, String pathAnnotationValue, List<String> path) {
        Matcher matcher = pathVariablePattern.matcher(pathAnnotationValue);
        List<String> pathVariablesNames = new ArrayList<>();
        while (matcher.find())
            pathVariablesNames.add(matcher.group(1));

        Parameter requestBodyParameter = getRequestBodyParameter(method);
        Object requestBody = null;
        if (requestBodyParameter != null)
            requestBody = tryToSerializeRequestBody(requestBodyParameter.getType(), req);

        Object[] parameterValues = new String[method.getParameters().length];
        for (int i = 0; i < parameterValues.length; i++) {
            if (method.getParameters()[i].isAnnotationPresent(RequestBody.class))
                parameterValues[i] = requestBody;
        }

        List<String> annotatedParametersNames = getPathVariableAnnotatedParametersNames(method);
        if (annotatedParametersNames.size() > 0) {
            for (int i = 0; i < path.size(); i++) {
                String pathVariable = path.get(i).substring(1); // remove the '/' from the path variable
                if (pathVariablesNames.get(i).equals(annotatedParametersNames.get(i)))
                    parameterValues[i] = pathVariable;
            }
        }

        return parameterValues;
    }

    private String getAnnotationValue(Method method, String httpMethod) {
        return switch (httpMethod) {
            case "GET" -> method.getAnnotation(GetMapping.class).value();
            case "POST" -> method.getAnnotation(PostMapping.class).value();
            case "PUT" -> method.getAnnotation(PutMapping.class).value();
            case "DELETE" -> method.getAnnotation(DeleteMapping.class).value();
            default -> "";
        };
    }

    private Parameter getRequestBodyParameter(Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                return parameter;
            }
        }
        return null;
    }

    private <T> T tryToSerializeRequestBody(Class<T> type, HttpServletRequest req) {
        try {
            return gson.fromJson(req.getReader(), type);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> getPathVariableAnnotatedParametersNames(Method method) {
        List<String> result = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                String name = parameter.getName();
                result.add(name);
            }
        }
        return result;
    }
}
