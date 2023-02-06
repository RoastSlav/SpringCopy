package Spring;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class SpringApplication {
    public static void run(Class<?> postSpringApplicationClass, String[] args) throws Exception {
        DepContainerLoader depContainerLoader = new DepContainerLoader(postSpringApplicationClass);
        depContainerLoader.loadDependencies();

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

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("ResponseSwapFilter");
        filterDef.setFilterClass("Spring.ResponseSwapFilter");
        context.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.addServletName("Dispatcher");
        filterMap.addURLPattern("/*");
        filterMap.setFilterName("ResponseSwapFilter");
        context.addFilterMap(filterMap);

        return tomcat;
    }
}
