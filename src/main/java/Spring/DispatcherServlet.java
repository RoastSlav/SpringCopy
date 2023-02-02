package Spring;

import DepInj.Container;
import DepInj.RegistryException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {
    Container container = Container.getContainer();
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String[] pathParts = pathInfo.split("/");
        String controllerMapping = pathParts[1];
        String methodMapping = pathParts[2];

        try {
            Object instance = container.getInstance(controllerMapping);
            if (instance == null)
                throw new RuntimeException("Controller not found");

        } catch (RegistryException e) {
            throw new RuntimeException(e);
        }
    }
}
