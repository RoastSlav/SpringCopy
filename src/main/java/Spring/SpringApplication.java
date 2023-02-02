package Spring;

import DepInj.Container;
import DepInj.Inject;
import DepInj.RegistryException;
import Spring.Anotations.Component;
import Spring.Anotations.Controller;
import Spring.Anotations.RestController;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.Set;

import static org.reflections.scanners.Scanners.ConstructorsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

public class SpringApplication {
    private static final Properties properties = findPropertiesFile();
    private static final SqlSessionFactory sessionFactory;
    private static final Reflections reflections = new Reflections("");

    static {
        try {
            if (properties == null)
                throw new RuntimeException("The properties file was not found in the usual location");

            String mBatisResource = properties.getProperty("mb_resource");
            try (Reader reader = Resources.getResourceAsReader(mBatisResource)) {
                sessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
            }
        } catch (IOException e) {
            throw new RuntimeException("There was an error initializing the SqlSessionFactory");
        }
    }

    public static void run(Class<?> postSpringApplicationClass, String[] args) throws Exception {
        DepContainerLoader depContainerLoader = new DepContainerLoader(sessionFactory);

        Set<Class<?>> controllers = reflections.get(TypesAnnotated.with(Controller.class).asClass());
        Set<Class<?>> restControllers = reflections.get(TypesAnnotated.with(RestController.class).asClass());
        controllers.addAll(restControllers);
        depContainerLoader.loadControllers(controllers);

        Set<Class<?>> mappers = reflections.get(TypesAnnotated.with(Mapper.class).asClass());
        depContainerLoader.loadMappers(mappers);

        Set<Class<?>> components = reflections.get(TypesAnnotated.with(Component.class).asClass());
        depContainerLoader.loadComponents(components);
    }

    private static Properties findPropertiesFile() {
        //TODO: search for properties file in the resources folder
        return null;
    }
}
