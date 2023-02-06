package Spring;

import DepInj.Container;
import Spring.Anotations.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

class DepContainerLoader {
    private static final String PACKAGE_PATH = "";
    Container container = Container.getContainer();

    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) throws ClassNotFoundException, IOException {
        Set<Class<?>> annotatedClasses = new HashSet<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(PACKAGE_PATH);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            File folder = new File(filePath);

            if (folder.isDirectory()) {
                Set<Class<?>> annotatedClassesFromFolder = getAnnotatedClassesFromFolder(folder, PACKAGE_PATH, annotation);
                annotatedClasses.addAll(annotatedClassesFromFolder);
            }
        }
        return annotatedClasses;
    }

    private Set<Class<?>> getAnnotatedClassesFromFolder(File folder, String packageName, Class<? extends Annotation> annotation) throws ClassNotFoundException {
        Set<Class<?>> annotatedClasses = new HashSet<>();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                String packageToSearch = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                Set<Class<?>> annotatedClassesFromFolder = getAnnotatedClassesFromFolder(file, packageToSearch, annotation);
                annotatedClasses.addAll(annotatedClassesFromFolder);
                continue;
            }

            if (file.isFile() && file.getName().endsWith(".class")) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                String className = file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = classLoader.loadClass(packageName + "." + className);
                if (clazz.isAnnotationPresent(annotation))
                    annotatedClasses.add(clazz);
            }
        }
        return annotatedClasses;
    }

    public void loadBeans() throws Exception {
        Set<Class<?>> beans = getAnnotatedClasses(Bean.class);
        for (Class<?> bean : beans) {
            container.registerImplementation(bean);
            Object instance = container.getInstance(bean);
            container.registerInstance(bean, instance);
        }
    }

    public void loadMappers(SqlSessionFactory fac) throws Exception {
        Set<Class<?>> mappers = getAnnotatedClasses(Mapper.class);
        Configuration configuration = fac.getConfiguration();
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
            try (SqlSession session = fac.openSession()) {
                Object mapperInstance = session.getMapper(mapper);
                container.registerNoInjectInstance(mapper, mapperInstance);
            }
        }
    }

    public void loadControllers() throws Exception {
        Set<Class<?>> controllers = getAnnotatedClasses(Controller.class);
        Set<Class<?>> restControllers = getAnnotatedClasses(RestController.class);
        controllers.addAll(restControllers);

        for (Class<?> controller : controllers) {
            container.registerImplementation(controller);
            Object instance = container.getInstance(controller);
            if (controller.isAnnotationPresent(RequestMapping.class)) {
                String value = controller.getAnnotation(RequestMapping.class).value();
                if (value == null)
                    value = "";

                container.registerInstance(value, instance);
                return;
            }
            container.registerInstance(controller, instance);
        }
    }

    public void loadComponents() throws Exception {
        Set<Class<?>> components = getAnnotatedClasses(Component.class);
        for (Class<?> component : components) {
            container.registerImplementation(component);
        }
    }
}
