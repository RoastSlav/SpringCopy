package Spring;

import Spring.Anotations.Bean;
import Spring.Anotations.Component;
import Spring.Anotations.ComponentScan;
import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;
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
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DepContainerLoader {
    private final List<String> packagesToBeScanned = new ArrayList<>();
    ApplicationContext context;

    public DepContainerLoader(Class<?> mainClass, ApplicationContext context) throws NoSuchBeanDefinitionException, BeansException {
        this.context = context;
        getPackagesToLoad(mainClass);
        packagesToBeScanned.add(0, mainClass.getPackageName());
        context.registerBean("packagesToBeScanned", packagesToBeScanned);
    }

    public void getPackagesToLoad(Class<?> mainClass) {
        if (!mainClass.isAnnotationPresent(ComponentScan.class))
            return;

        ComponentScan componentScan = mainClass.getAnnotation(ComponentScan.class);
        List<String> packages = Arrays.stream(componentScan.value()).toList();
        packagesToBeScanned.stream().filter(packages::contains).forEach(packagesToBeScanned::remove);
        packagesToBeScanned.addAll(packages);

        if (!packagesToBeScanned.contains(mainClass.getPackageName()))
            packagesToBeScanned.add(mainClass.getPackageName());
    }

    public void loadDependencies() throws Exception {
        for (String packageToScan : packagesToBeScanned) {
            loadMappers(packageToScan);
            loadConfigurations(packageToScan);
            loadComponents(packageToScan);
        }
    }

    private SqlSessionFactory getSqlSessionFactory() throws IOException {
        Properties properties = ResourceFileSearcher.getPropertiesFile();

        String mBatisResource = properties.getProperty("mb_resource");
        try (Reader reader = Resources.getResourceAsReader(mBatisResource)) {
            return new SqlSessionFactoryBuilder().build(reader, properties);
        }
    }

    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation, String packageToScan) throws ClassNotFoundException, IOException {
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

    private void loadConfigurations(String packageToScan) throws Exception {
        Set<Class<?>> configurations = getAnnotatedClasses(Spring.Anotations.Configuration.class, packageToScan);
        for (Class<?> config : configurations) {
            getPackagesToLoad(config);

            Constructor<?> declaredConstructor = config.getDeclaredConstructor();
            if (declaredConstructor.isAnnotationPresent(Bean.class)) {
                Object instance = context.beanCreator.getInstance(config);
                context.registerBean(config, instance);
            }

            Field[] declaredFields = config.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Bean.class)) {
                    Object instance = context.beanCreator.getInstance(field.getType());
                    context.registerBean(field.getType(), instance);
                }
            }
        }
    }

    private void loadMappers(String packageToScan) throws Exception {
        SqlSessionFactory fac = getSqlSessionFactory();
        Set<Class<?>> mappers = getAnnotatedClasses(Mapper.class, packageToScan);
        Configuration configuration = fac.getConfiguration();
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
            Object mapperProxy = Proxy.newProxyInstance(Mapper.class.getClassLoader(),
                    new Class[]{mapper},
                    new MapperInvocationHandler(fac, mapper));
            context.registerBean(mapper, mapperProxy);
        }
    }

    private void loadComponents(String packageToScan) throws Exception {
        Set<Class<?>> components = getAnnotatedClasses(Component.class, packageToScan);
        for (Class<?> component : components) {
            getPackagesToLoad(component);
            context.registerImplementation(component);
        }
    }

    private static class MapperInvocationHandler implements InvocationHandler {
        private final SqlSessionFactory fac;
        private final Class<?> mapper;

        public MapperInvocationHandler(SqlSessionFactory fac, Class<?> mapper) {
            this.fac = fac;
            this.mapper = mapper;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try (SqlSession session = fac.openSession()) {
                Object mapperInstance = session.getMapper(mapper);
                return method.invoke(mapperInstance, args);
            }
        }
    }
}