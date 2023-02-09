package Spring;

import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {
    //TODO: load properties into the context
    protected final Map<String, Object> singletons = new HashMap<>();
    protected final Map<Class<?>, Class<?>> implementations = new HashMap<>();
    protected final Map<String, ObjectProvider<?>> prototypes = new HashMap<>();
    protected final BeanCreator beanCreator = new BeanCreator(this);
    private final LocalDate localDate = LocalDate.now();
    private String ApplicationName;

    public BeanCreator getBeanCreator() {
        return beanCreator;
    }

    public String getApplicationName() {
        return ApplicationName;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public boolean containsBean(String beanName) {
        return singletons.containsKey(beanName) || prototypes.containsKey(beanName);
    }

    public Object autowireBean(Object bean) throws Exception {
        beanCreator.decorateInstance(bean);
        return bean;
    }

    public String[] getAliases(String name) throws NoSuchBeanDefinitionException, BeansException {
        Object bean = getBean(name);
        List<String> aliases = new ArrayList<>();
        for (Map.Entry<String, Object> entry : singletons.entrySet()) {
            if (entry.getValue() == bean)
                aliases.add(entry.getKey());
        }
        for (Map.Entry<String, ObjectProvider<?>> entry : prototypes.entrySet()) {
            if (entry.getValue().getType() == bean.getClass())
                aliases.add(entry.getKey());
        }
        return aliases.toArray(String[]::new);
    }

    public <T> Object getBean(Class<T> type) throws NoSuchBeanDefinitionException, BeansException {
        return getBean(type.getName(), type);
    }

    public Object getBean(String name) throws NoSuchBeanDefinitionException, BeansException {
        if (singletons.containsKey(name))
            return singletons.get(name);
        if (prototypes.containsKey(name))
            return prototypes.get(name).getObject();
        throw new NoSuchBeanDefinitionException("No bean with name " + name + " found");
    }

    public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException, BeansException {
        if (singletons.containsKey(name)) {
            if (requiredType.isAssignableFrom(singletons.get(name).getClass()))
                return (T) singletons.get(name);
        }
        if (prototypes.containsKey(name)) {
            if (requiredType.isAssignableFrom(prototypes.get(name).getType()))
                return (T) prototypes.get(name).getObject();
        }
        throw new NoSuchBeanDefinitionException("No bean of type " + requiredType.getName() + " with name " + name + " found");
    }

    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws NoSuchBeanDefinitionException {
        if (prototypes.containsKey(requiredType.getName()))
            return (ObjectProvider<T>) prototypes.get(requiredType.getName());
        throw new NoSuchBeanDefinitionException("No bean of type " + requiredType.getName() + " found");
    }

    public Class<?> getType(String name) {
        if (singletons.containsKey(name))
            return singletons.get(name).getClass();
        if (prototypes.containsKey(name))
            return prototypes.get(name).getType();
        return null;
    }

    public boolean isPrototype(String name) {
        return prototypes.containsKey(name);
    }

    public boolean isSingleton(String name) {
        return singletons.containsKey(name);
    }

    public boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException, BeansException {
        Object bean = getBean(name);
        return targetType.isAssignableFrom(bean.getClass());
    }

    public void registerBean(String name, Object bean) throws NoSuchBeanDefinitionException, BeansException {
        singletons.put(name, bean);
    }

    protected void registerBean(Class<?> type, Object bean) throws NoSuchBeanDefinitionException, BeansException {
        singletons.put(type.getName(), bean);
    }

    protected void registerImplementation(Class<?> main, Class<?> sub) {
        implementations.put(main, sub);
    }

    protected void registerImplementation(Class<?> clazz) {
        registerImplementation(clazz, clazz);
    }

    protected void registerPrototype(String name, ObjectProvider<?> provider) {
        prototypes.put(name, provider);
    }
}
