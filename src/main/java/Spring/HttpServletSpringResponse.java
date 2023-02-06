package Spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class HttpServletSpringResponse extends HttpServletResponseWrapper {
    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to be wrapped
     * @throws IllegalArgumentException if the response is null
     */
    public HttpServletSpringResponse(HttpServletResponse response) {
        super(response);
    }

    public void sendError(int sc, String msg, HttpServletRequest request) throws IOException {
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        String responseBody = String.format("{\"timestamp\": \"%s\",\"status\": %d,\"error\": \"%s\",\"path\": \"%s\"}",
                date , sc, msg, request.getRequestURI());

        PrintWriter writer = super.getWriter();
        super.setContentType("application/json");
        super.setCharacterEncoding("UTF-8");
        super.setStatus(sc);

        writer.println(responseBody);
        writer.flush();
    }
}
