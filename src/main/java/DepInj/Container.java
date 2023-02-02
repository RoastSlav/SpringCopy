package DepInj;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings({"unchecked"})
public class Container {
    private static final Container con = new Container();
    private final Map<String, Object> instances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();

    private Container() {
    }

    public static Container getContainer() {
        return con;
    }

    public Object getInstance(String key) throws RegistryException {
        Object instance = instances.get(key);
        if (instance == null)
            throw new RegistryException("No instance registered for key: " + key);
        return instance;
    }

    public <T> T getInstance(Class<T> type) throws RegistryException {
        return getInstance(type, new HashSet<>());
    }

    private <T> T getInstance(Class<T> type, HashSet<Class<?>> visited) throws RegistryException {
        T instance = (T) instances.get(type.getName());
        if (instance == null) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
                return (T) getInterfaceInstance(type);

            instance = (T) createInstance(type, visited);
            instances.put(type.getName(), instance);
            return instance;
        }
        return instance;
    }

    private Object getInterfaceInstance(Class<?> c) throws RegistryException {
        Class<?> impl = implementations.get(c);
        if (impl == null && c.isAnnotationPresent(Default.class))
            impl = c.getAnnotation(Default.class).value();

        if (impl == null)
            throw new RegistryException("No implementation registered for interface: " + c.getName());

        Object instance = getInstance(impl, new HashSet<>());
        instances.put(c.getName(), instance);
        return instance;
    }

    public void decorateInstance(Object o) throws Exception {
        injectFieldsIntoInstance(o, new HashSet<>());
    }

    public void registerInstance(String key, Object instance) throws Exception {
        injectFieldsIntoInstance(instance, new HashSet<>());
        instances.put(key, instance);
    }

    public void registerInstance(Class c, Object instance) throws Exception {
        injectFieldsIntoInstance(instance, new HashSet<>());
        instances.put(c.getName(), instance);
    }

    public void registerImplementation(Class<?> c) throws RegistryException {
        registerImplementation(c, c);
    }

    public void registerImplementation(Class<?> c, Class<?> subClass) throws RegistryException {
        if (c.isInterface()) {
            Class<?> impl = implementations.get(c);
            if (impl != null && !impl.equals(subClass)) {
                throw new RegistryException("Implementation of " + subClass.getName() + " has changed");
            }
        }
        implementations.put(c, subClass);
    }

    private Constructor<?> getConstructor(Class<?> c) throws NoSuchMethodException, RegistryException {
        Constructor<?> constructor = null;

        Constructor<?>[] declaredConstructors = c.getDeclaredConstructors();
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (!declaredConstructor.isAnnotationPresent(Inject.class))
                continue;

            if (constructor != null)
                throw new RegistryException("Multiple constructors annotated with @Inject");

            constructor = declaredConstructor;
        }

        if (constructor == null)
            constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        return constructor;
    }

    private Object[] getConstructorParams(Constructor<?> constructor) throws RegistryException {
        Parameter[] parameterTypes = constructor.getParameters();
        Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isAnnotationPresent(Named.class)) {
                String name = parameterTypes[i].getName();
                params[i] = getInstance(name);
                continue;
            }

            Class<?> parameterType = parameterTypes[i].getType();
            try {
                params[i] = getInstance(parameterType);
            } catch (Exception e) {
                throw new RegistryException("Failed to get instance for constructor parameter", e);
            }
        }
        return params;
    }

    private Object createInstance(Class<?> type, HashSet<Class<?>> visited) throws RegistryException {
        Object instance;
        try {
            Constructor<?> constructor = getConstructor(type);
            Object[] params = getConstructorParams(constructor);
            instance = constructor.newInstance(params);
            injectFieldsIntoInstance(instance, visited);
        } catch (RegistryException e) {
            throw new RegistryException("Failed to inject the fields into the instance", e);
        } catch (Exception e) {
            throw new RegistryException("Failed to create instance for object: " + type.getName(), e);
        }

        if (instance instanceof Initializer) {
            try {
                ((Initializer) instance).init();
            } catch (Exception e) {
                throw new RegistryException("Failed to initialize instance for object: " + type.getName(), e);
            }
        }
        return instance;
    }

    private void setLazyObject(Object instance, Field field) throws RegistryException {
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
            throw new RegistryException("Failed to set lazy object", e);
        }
    }

    private void injectFieldsIntoInstance(Object instance, HashSet<Class<?>> visited) throws RegistryException {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (visited.contains(instance.getClass())) {
                setLazyObject(instance, field);
                continue;
            }

            visited.add(instance.getClass());

            if (!field.isAnnotationPresent(Inject.class))
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

    private void setPrimitiveFieldValue(Object instance, Field field) throws RegistryException {
        Object value = null;
        if (field.isAnnotationPresent(Named.class)) {
            String annotationValue = field.getAnnotation(Named.class).value();
            if (annotationValue != null && !annotationValue.isEmpty())
                value = instances.get(annotationValue);
            else
                value = instances.get(field.getName());
        }

        if (value == null)
            throw new RegistryException("No value found for primitive field: " + field.getName());

        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RegistryException("Failed to set value for primitive field: " + field.getName(), e);
        }
    }

    private void setObjectFieldValue(Object instance, Field field, HashSet<Class<?>> visited) throws RegistryException {
        if (!field.isAnnotationPresent(Named.class)) {
            Object inject = getInstance(field.getType(), visited);
            try {
                field.set(instance, inject);
            } catch (IllegalAccessException e) {
                throw new RegistryException("Failed to inject field: " + field.getName(), e);
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
            throw new RegistryException("Failed to inject field: " + field.getName(), e);
        }
    }
}
