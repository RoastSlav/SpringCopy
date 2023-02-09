package Spring;

import Spring.Anotations.*;
import Spring.Exceptions.BeansException;
import Spring.Exceptions.NoSuchBeanDefinitionException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Map;

public class BeanCreator {
    Map<Class<?>, Class<?>> implementations;
    Map<String, Object> singletons;

    public BeanCreator(ApplicationContext context) {
        implementations = context.implementations;
        singletons = context.singletons;
    }

    public void decorateInstance(Object instance) throws NoSuchBeanDefinitionException, BeansException {
        injectFieldsIntoInstance(instance, new HashSet<>());
    }

    private Object getInstance(String key) throws NoSuchBeanDefinitionException {
        Object instance = singletons.get(key);
        if (instance == null)
            throw new NoSuchBeanDefinitionException("No instance registered for key: " + key);
        return instance;
    }

    public <T> T getInstance(Class<T> type) throws NoSuchBeanDefinitionException, BeansException {
        return getInstance(type, new HashSet<>());
    }

    private <T> T getInstance(Class<T> type, HashSet<Class<?>> visited) throws NoSuchBeanDefinitionException, BeansException {
        T instance = (T) singletons.get(type.getName());
        if (instance == null) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
                return (T) getInterfaceInstance(type);

            instance = (T) createInstance(type, visited);
        }
        return instance;
    }

    private Object getInterfaceInstance(Class<?> c) throws NoSuchBeanDefinitionException, BeansException {
        Class<?> impl = implementations.get(c);
        if (impl == null && c.isAnnotationPresent(Default.class))
            impl = c.getAnnotation(Default.class).value();

        if (impl == null)
            throw new NoSuchBeanDefinitionException("No implementation registered for interface: " + c.getName());

        Object instance = getInstance(impl, new HashSet<>());
        return instance;
    }

    private Constructor<?> getConstructor(Class<?> c) throws NoSuchMethodException, BeansException {
        Constructor<?> constructor = null;

        Constructor<?>[] declaredConstructors = c.getDeclaredConstructors();
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (!declaredConstructor.isAnnotationPresent(Autowired.class))
                continue;

            if (constructor != null)
                throw new BeansException("Multiple constructors annotated with @Inject");

            constructor = declaredConstructor;
        }

        if (constructor == null)
            constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        return constructor;
    }

    private Object[] getConstructorParams(Constructor<?> constructor) throws BeansException, NoSuchBeanDefinitionException {
        Parameter[] parameterTypes = constructor.getParameters();
        Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isAnnotationPresent(Named.class)) {
                String name = parameterTypes[i].getName();
                String annotationValue = parameterTypes[i].getAnnotation(Named.class).value();
                if (annotationValue != "")
                    name = annotationValue;
                params[i] = getInstance(name);
                continue;
            }

            Class<?> parameterType = parameterTypes[i].getType();
            try {
                params[i] = getInstance(parameterType);
            } catch (Exception e) {
                throw new BeansException("Failed to get instance for constructor parameter", e);
            }
        }
        return params;
    }

    private Object createInstance(Class<?> type, HashSet<Class<?>> visited) throws BeansException {
        Object instance;
        try {
            Constructor<?> constructor = getConstructor(type);
            Object[] params = getConstructorParams(constructor);
            instance = constructor.newInstance(params);
            injectFieldsIntoInstance(instance, visited);
        } catch (NoSuchBeanDefinitionException e) {
            throw new BeansException("Failed to inject the fields into the instance", e);
        } catch (Exception e) {
            throw new BeansException("Failed to create instance for object: " + type.getName(), e);
        }

        if (instance instanceof Initializer) {
            try {
                ((Initializer) instance).init();
            } catch (Exception e) {
                throw new BeansException("Failed to initialize instance for object: " + type.getName(), e);
            }
        }
        return instance;
    }

    private void setLazyObject(Object instance, Field field) throws BeansException {
        Object mock = Mockito.mock(field.getType(), (Answer<?>) invocationOnMock -> {
            Named named = field.getDeclaredAnnotation(Named.class);
            Object value;
            if (named == null)
                value = getInstance(field.getType());
            else if (named.value() != null && !named.value().isEmpty())
                value = getInstance(named.value());
            else
                value = getInstance(field.getName());
            field.set(instance, value);
            return invocationOnMock.getMethod().invoke(value, invocationOnMock.getArguments());
        });

        try {
            field.setAccessible(true);
            field.set(instance, mock);
        } catch (IllegalAccessException e) {
            throw new BeansException("Failed to set lazy object", e);
        }
    }

    private void injectFieldsIntoInstance(Object instance, HashSet<Class<?>> visited) throws NoSuchBeanDefinitionException, BeansException {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class) && visited.contains(field.getClass())) {
                setLazyObject(instance, field);
                continue;
            }

            visited.add(instance.getClass());

            if (!field.isAnnotationPresent(Autowired.class))
                continue;

            field.setAccessible(true);

            if (field.isAnnotationPresent(Lazy.class)) {
                setLazyObject(instance, field);
                continue;
            }

            if (checkForPrimitive(field.getType())) {
                setPrimitiveFieldValue(instance, field);
                continue;
            }
            setObjectFieldValue(instance, field, visited);
        }
    }

    private boolean checkForPrimitive(Class<?> type) {
        return type.isPrimitive() ||
                type.equals(String.class) ||
                type.equals(Integer.class) ||
                type.equals(Long.class) ||
                type.equals(Double.class) ||
                type.equals(Float.class) ||
                type.equals(Boolean.class);
    }

    private void setPrimitiveFieldValue(Object instance, Field field) throws NoSuchBeanDefinitionException {
        Object value = null;
        if (field.isAnnotationPresent(Named.class)) {
            String annotationValue = field.getAnnotation(Named.class).value();
            if (annotationValue != null && !annotationValue.isEmpty())
                value = singletons.get(annotationValue);
            else
                value = singletons.get(field.getName());
        }

        if (value == null)
            throw new NoSuchBeanDefinitionException("No value found for primitive field: " + field.getName());

        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new NoSuchBeanDefinitionException("Failed to set value for primitive field: " + field.getName(), e);
        }
    }

    private void setObjectFieldValue(Object instance, Field field, HashSet<Class<?>> visited) throws NoSuchBeanDefinitionException, BeansException {
        if (!field.isAnnotationPresent(Named.class)) {
            Object inject = getInstance(field.getType(), visited);
            try {
                field.set(instance, inject);
            } catch (IllegalAccessException e) {
                throw new BeansException("Failed to inject field: " + field.getName(), e);
            }
            return;
        }

        String value = field.getAnnotation(Named.class).value();
        if (value == null || value.isEmpty())
            value = field.getName();
        Object inject = getInstance(value);
        try {
            field.set(instance, inject);
        } catch (IllegalAccessException e) {
            throw new BeansException("Failed to inject field: " + field.getName(), e);
        }
    }
}
