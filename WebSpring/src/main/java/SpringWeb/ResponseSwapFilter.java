package SpringWeb;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResponseSwapFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletSpringResponse springResponse = new HttpServletSpringResponse((HttpServletResponse) response);
        chain.doFilter(request, springResponse);
    }

    @Override
    public void destroy() {

    }
}
