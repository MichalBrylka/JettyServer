package org.nemesis;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

import jakarta.servlet.http.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@lombok.extern.slf4j.Slf4j
public class BookingServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final List<Booking> bookings = new CopyOnWriteArrayList<>(List.of(
            new Booking(1, "John Wick", "Continental Suite"),
            new Booking(2, "Ellen Ripley", "Stasis Pod"),
            new Booking(3, "Arthur Dent", "Heart of Gold Cabin")
    ));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");

        // GET /bookings
        if (pathInfo == null || pathInfo.equals("/")) {
            mapper.writeValue(resp.getWriter(), bookings);
            return;
        }

        // GET /bookings/{id}
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            bookings.stream()
                    .filter(b -> b.id == id)
                    .findFirst()
                    .ifPresentOrElse(
                            b -> {
                                try {
                                    mapper.writeValue(resp.getWriter(), b);
                                } catch (Exception e) {
                                    try {
                                        sendError(resp, 500, "Internal server error");
                                    } catch (IOException ioException) {
                                        // Silently fail
                                    }
                                }
                            },
                            () -> {
                                try {
                                    sendError(resp, 404, "Booking not found");
                                } catch (IOException ioException) {
                                    // Silently fail
                                }
                            }
                    );
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid ID format");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Booking newBooking = mapper.readValue(req.getReader(), Booking.class);
            if (newBooking.guestName == null || newBooking.guestName.isBlank()) {
                sendError(resp, 400, "Guest name is required");
                return;
            }
            bookings.add(newBooking);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Location", req.getRequestURI() + "/" + newBooking.id);
            mapper.writeValue(resp.getWriter(), newBooking);
        } catch (Exception e) {
            sendError(resp, 400, "Malformed JSON request");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. Check JWT Authentication
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token provided
            resp.setHeader("WWW-Authenticate",
                    """
                            Bearer realm="BookingAPI", \
                            error="invalid_token", \
                            error_description="JWT token required. Obtain a token via POST /login\"""");
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: JWT token required");
            return;
        }

        String token = authHeader.substring(7);

        if (!(isValidJWT(token) instanceof Role role)) {
            // Invalid or expired token
            resp.setHeader("WWW-Authenticate",
                    "Bearer realm=\"BookingAPI\", " +
                    "error=\"invalid_token\", " +
                    "error_description=\"Invalid or expired JWT. Obtain a new token via POST /login\"");
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid or expired token");
            return;
        }
        if (role != Role.ADMIN) {
            // Insufficient permissions
            resp.setHeader("WWW-Authenticate",
                    "Bearer realm=\"BookingAPI\", " +
                    "error=\"insufficient_scope\", " +
                    "error_description=\"Admin role required to delete bookings\"");
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Admin role required");
            return;
        }

        // 2. Validate the path
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed on collection");
            return;
        }

        // 3. Parse ID and remove booking
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            boolean removed = bookings.removeIf(b -> b.id == id);
            if (removed) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Booking not found");
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format");
        }
    }

    private Role isValidJWT(String token) {
        try {
            var claims = JwtService.parse(token);
            String roleText = claims.get("role", String.class);
            return Role.fromString(roleText);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    private void sendError(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json");
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", msg));
    }
}