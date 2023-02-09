package SpringWeb;

import Spring.Anotations.Autowired;
import Spring.Anotations.Initializer;
import Spring.Anotations.Named;
import Spring.ApplicationContext;
import Spring.DepContainerLoader;
import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;
import SpringWeb.Annotation.Controller;
import SpringWeb.Annotation.RestController;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.Servlet;
import java.io.File;
import java.util.List;
import java.util.Set;

import static Spring.SpringApplication.applicationContext;

public class Web implements Initializer {
    private static final String PACKAGE_TO_SCAN = ""; //Scans all packages
    DispatcherServlet instance = new DispatcherServlet();
    @Autowired
    DepContainerLoader depContainerLoader;
    @Autowired
    ApplicationContext context;
    @Autowired
    @Named()
    List<String> packagesToBeScanned;

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
        filterDef.setFilterName("SpringWeb.ResponseSwapFilter");
        filterDef.setFilterClass("SpringWeb.ResponseSwapFilter");
        context.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.addServletName("Dispatcher");
        filterMap.addURLPattern("/*");
        filterMap.setFilterName("SpringWeb.ResponseSwapFilter");
        context.addFilterMap(filterMap);

        return tomcat;
    }

    private void loadControllers() throws Exception {
        ControllerManager conManager = new ControllerManager();
        context.registerBean(ControllerManager.class.getName(), conManager);
        for (String packageToBeScanned : packagesToBeScanned) {
            Set<Class<?>> controllers = depContainerLoader.getAnnotatedClasses(Controller.class, packageToBeScanned);
            Set<Class<?>> restControllers = depContainerLoader.getAnnotatedClasses(RestController.class, packageToBeScanned);
            controllers.addAll(restControllers);
            conManager.loadControllers(controllers);
        }
    }

    @Override
    public void init() throws Exception {
        applicationContext.registerBean(DispatcherServlet.class.getName(), instance);
        loadControllers();

        Tomcat tomcat = setupTomcat();
        tomcat.start();
        tomcat.getServer().await();
    }
}
