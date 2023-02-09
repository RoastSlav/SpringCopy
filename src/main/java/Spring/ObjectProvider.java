package Spring;

import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;

public class ObjectProvider<T> {
    private Class<T> type;
    private BeanCreator beanCreator;

    protected ObjectProvider(Class<T> type, BeanCreator beanCreator) {
        this.type = type;
        this.beanCreator = beanCreator;
    }

    public Class<T> getType() {
        return type;
    }

    public T getObject() throws NoSuchBeanDefinitionException, BeansException {
        return beanCreator.getInstance(type);
    }
}
