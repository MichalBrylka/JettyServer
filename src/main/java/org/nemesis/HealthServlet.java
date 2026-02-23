package org.nemesis;

import jakarta.servlet.http.*;
import java.io.IOException;

public class HealthServlet extends HttpServlet {

    // GET /health returns a JSON status
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().print("{\"status\":\"UP\"}");
    }

    // HEAD /health returns only the headers (200 OK)
    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
    }
}
