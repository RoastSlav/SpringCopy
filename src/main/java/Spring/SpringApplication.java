package Spring;

import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;
import Spring.Web.DispatcherServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.Servlet;
import java.io.File;

public class SpringApplication {
    public final static ApplicationContext applicationContext = new ApplicationContext();
    public static void run(Class<?> postSpringApplicationClass, String[] args) throws Exception {
        DispatcherServlet instance = applicationContext.beanCreator.getInstance(DispatcherServlet.class);
        applicationContext.registerBean(DispatcherServlet.class, instance);
        instance.init();

        Tomcat tomcat = setupTomcat();

        DepContainerLoader depContainerLoader = new DepContainerLoader(postSpringApplicationClass, applicationContext);
        depContainerLoader.loadDependencies();

        tomcat.start();
        tomcat.getServer().await();
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private static Tomcat setupTomcat() throws NoSuchBeanDefinitionException, BeansException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();

        Context context = tomcat.addContext(contextPath, docBase);

        Servlet dispatcherServlet = (Servlet) applicationContext.getBean(DispatcherServlet.class);
        Tomcat.addServlet(context, "Dispatcher", dispatcherServlet);
        context.addServletMappingDecoded("/*", "Dispatcher");

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("ResponseSwapFilter");
        filterDef.setFilterClass("Spring.Web.ResponseSwapFilter");
        context.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.addServletName("Dispatcher");
        filterMap.addURLPattern("/*");
        filterMap.setFilterName("ResponseSwapFilter");
        context.addFilterMap(filterMap);

        return tomcat;
    }
}
