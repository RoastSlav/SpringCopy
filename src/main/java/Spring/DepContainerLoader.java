package Spring;

import DepInj.Container;
import DepInj.RegistryException;
import Spring.Anotations.RequestMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.Set;

class DepContainerLoader {
    Container container = Container.getContainer();
    SqlSessionFactory fac;

    public DepContainerLoader(SqlSessionFactory fac) {
        this.fac = fac;
    }

    public void loadMappers(Set<Class<?>> mappers) throws Exception {
        for (Class<?> mapper : mappers) {
            Object mock = Mockito.mock(mapper, (Answer<?>) invocationOnMock -> {
                Object value;
                try (SqlSession session = fac.openSession()) {
                    value = session.getMapper(mapper);
                }
                return invocationOnMock.getMethod().invoke(value, invocationOnMock.getArguments());
            });
            container.registerInstance(mapper, mock);
        }
    }

    public void loadControllers(Set<Class<?>> controllers) throws Exception {
        for (Class<?> controller : controllers) {
            container.registerImplementation(controller);
            Object instance = container.getInstance(controller);
            if (controller.isAnnotationPresent(RequestMapping.class)) {
                String value = controller.getAnnotation(RequestMapping.class).value();
                if (value == null)
                    value = "";

                container.registerInstance(value, instance);
                return;
            }
            container.registerInstance(controller, instance);
        }
    }

    public void loadComponents(Set<Class<?>> components) throws RegistryException {
        for (Class<?> component : components) {
            container.registerImplementation(component);
        }
    }
}
