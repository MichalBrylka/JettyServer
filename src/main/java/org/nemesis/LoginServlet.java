package org.nemesis;

import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class LoginServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, User> USERS = Map.of(
            "admin", new User("admin", "password", Role.ADMIN),
            "john", new User("john", "1234", Role.USER)
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)            throws IOException {
        Map<String, String> credentials =
                mapper.readValue(req.getInputStream(), Map.class);

        String username = credentials.get("username");
        String password = credentials.get("password");

        User user = USERS.get(username);
        if (user == null || !user.password.equals(password)) {
            // Authentication failed
            resp.setHeader("WWW-Authenticate", "Basic realm=\"BookingAPI\"");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(resp.getOutputStream(),                    Map.of("error", "Invalid username or password"));
            return;
        }

        // Authentication successful: issue JWT
        String token = JwtService.createToken(username, user.role);

        resp.setContentType("application/json");
        mapper.writeValue(resp.getOutputStream(), Map.of("token", token));
    }

    public record User(String username, String password, Role role) {
    }
}