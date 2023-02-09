package Spring.Web;

import Spring.Anotations.*;
import Spring.BeanCreator;
import Spring.SpringApplication;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ControllerManager {
    private final Map<String, ControllerRegistration> mappings = new HashMap<>();
    private final BeanCreator beanCreator = SpringApplication.applicationContext.getBeanCreator();

    public void loadControllers(Set<Class<?>> controllers) {
        for (Class<?> controller : controllers) {
            ControllerRegistration registration = new ControllerRegistration();

            try {
                registration.instance = beanCreator.getInstance(controller);
                SpringApplication.applicationContext.registerBean(controller.getName(), registration.instance);
            } catch (Exception e) {
                System.out.println("Could not load the controller: " + controller.getName());
                e.printStackTrace();
                continue;
            }

            if (controller.isAnnotationPresent(RestController.class)) {
                RequestMapping restController = controller.getAnnotation(RequestMapping.class);
                registration.isRest = true;
                registration.path = restController.value();
                registration.name = restController.getClass().getName();
            } else if (controller.isAnnotationPresent(Controller.class)) {
                RequestMapping restController = controller.getAnnotation(RequestMapping.class);
                registration.path = restController.value();
                registration.name = restController.getClass().getName();
            }
            loadMethods(controller, registration);
            mappings.put(registration.path, registration);
        }
    }

    private void loadMethods(Class<?> controller, ControllerRegistration registration) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                registration.getMappings.put(getMapping.value(), method);
            }
            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                registration.postMappings.put(postMapping.value(), method);
            }
            if (method.isAnnotationPresent(PutMapping.class)) {
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                registration.putMappings.put(putMapping.value(), method);
            }
            if (method.isAnnotationPresent(DeleteMapping.class)) {
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                registration.deleteMappings.put(deleteMapping.value(), method);
            }
        }
    }

    public ControllerRegistration getController(String controllerName) {
        return mappings.get(controllerName);
    }

    public boolean hasController(String controllerName) {
        return mappings.containsKey(controllerName);
    }
}
