package Spring;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class SpringApplication {
    private static final Properties properties;
    private static final SqlSessionFactory sessionFactory;

    static {
        try {
            properties = ResourceFileSearcher.getPropertiesFile();

            String mBatisResource = properties.getProperty("mb_resource");
            try (Reader reader = Resources.getResourceAsReader(mBatisResource)) {
                sessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
            }
        } catch (IOException e) {
            throw new RuntimeException("There was an error starting the SpringCopy application", e);
        }
    }

    public static void run(Class<?> postSpringApplicationClass, String[] args) throws Exception {
        DepContainerLoader depContainerLoader = new DepContainerLoader();
        depContainerLoader.loadMappers(sessionFactory);
        depContainerLoader.loadBeans();
        depContainerLoader.loadComponents();
        depContainerLoader.loadControllers();

        Tomcat tomcat = setupTomcat();
        tomcat.start();
        tomcat.getServer().await();
    }

    private static Tomcat setupTomcat() {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();

        Context context = tomcat.addContext(contextPath, docBase);

        Tomcat.addServlet(context, "Dispatcher", new DispatcherServlet());
        context.addServletMappingDecoded("/*", "Dispatcher");

        return tomcat;
    }
}
