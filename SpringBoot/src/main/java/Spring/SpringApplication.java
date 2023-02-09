package Spring;

public class SpringApplication {
    public final static ApplicationContext applicationContext = new ApplicationContext();

    public static void run(Class<?> postSpringApplicationClass, String[] args) throws Exception {
        applicationContext.registerBean(ApplicationContext.class.getName(), applicationContext);
        DepContainerLoader depContainerLoader = new DepContainerLoader(postSpringApplicationClass, applicationContext);
        applicationContext.registerBean(DepContainerLoader.class.getName(), depContainerLoader);
        depContainerLoader.loadDependencies();
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
