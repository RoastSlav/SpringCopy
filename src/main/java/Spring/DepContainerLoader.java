package Spring;

import DepInj.Container;
import Spring.Anotations.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

class DepContainerLoader {
    private final List<String> packagesToBeScanned = new ArrayList<>();
    Container container = Container.getContainer();

    public DepContainerLoader(Class<?> mainClass) {
        if (mainClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = mainClass.getAnnotation(ComponentScan.class);
            packagesToBeScanned.addAll(Arrays.stream(componentScan.value()).toList());
        }
        packagesToBeScanned.add(0, mainClass.getPackageName());
    }

    public void loadDependencies() throws Exception {
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
        for (String packageToScan : packagesToBeScanned)
            loadMappers(sqlSessionFactory, packageToScan);

        for (String packageToScan : packagesToBeScanned)
            loadBeans(packageToScan);

        for (String packageToScan : packagesToBeScanned)
            loadComponents(packageToScan);

        for (String packageToScan : packagesToBeScanned)
            loadControllers(packageToScan);
    }

    private SqlSessionFactory getSqlSessionFactory() throws IOException {
        Properties properties = ResourceFileSearcher.getPropertiesFile();

        String mBatisResource = properties.getProperty("mb_resource");
        try (Reader reader = Resources.getResourceAsReader(mBatisResource)) {
            return new SqlSessionFactoryBuilder().build(reader, properties);
        }
    }

    private Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation, String packageToScan) throws ClassNotFoundException, IOException {
        Set<Class<?>> annotatedClasses = new HashSet<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packageToScan);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            File folder = new File(filePath);

            if (folder.isDirectory()) {
                Set<Class<?>> annotatedClassesFromFolder = getAnnotatedClassesFromFolder(folder, packageToScan, annotation);
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

    private void loadBeans(String packageToScan) throws Exception {
        Set<Class<?>> beans = getAnnotatedClasses(Bean.class, packageToScan);
        for (Class<?> bean : beans) {
            container.registerImplementation(bean);
            Object instance = container.getInstance(bean);
            container.registerInstance(bean, instance);
        }
    }

    private void loadMappers(SqlSessionFactory fac, String packageToScan) throws Exception {
        Set<Class<?>> mappers = getAnnotatedClasses(Mapper.class, packageToScan);
        Configuration configuration = fac.getConfiguration();
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
            try (SqlSession session = fac.openSession()) {
                Object mapperInstance = session.getMapper(mapper);
                container.registerNoInjectInstance(mapper, mapperInstance);
            }
        }
    }

    private void loadControllers(String packageToScan) throws Exception {
        Set<Class<?>> controllers = getAnnotatedClasses(Controller.class, packageToScan);
        Set<Class<?>> restControllers = getAnnotatedClasses(RestController.class, packageToScan);
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

    private void loadComponents(String packageToScan) throws Exception {
        Set<Class<?>> components = getAnnotatedClasses(Component.class, packageToScan);
        for (Class<?> component : components) {
            container.registerImplementation(component);
        }
    }
}
