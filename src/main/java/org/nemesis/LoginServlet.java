package org.nemesis;

import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class LoginServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Map<String, String> credentials =
                mapper.readValue(req.getInputStream(), Map.class);

        String username = credentials.get("username");
        String password = credentials.get("password");

        if (!authenticate(username, password)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = JwtService.createToken(username);

        resp.setContentType("application/json");

        mapper.writeValue(resp.getOutputStream(),
                Map.of("token", token));
    }

    private boolean authenticate(String user, String pass) {
        return "admin".equals(user) && "password".equals(pass);
    }
}