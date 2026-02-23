package org.nemesis;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

import jakarta.servlet.http.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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
        // 1. Check Authentication
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !isValid(authHeader)) {
            resp.setHeader("WWW-Authenticate", "Basic realm=\"BookingAPI\"");
            sendError(resp, 401, "Unauthorized: Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, 405, "Method Not Allowed on collection");
            return;
        }

        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            boolean removed = bookings.removeIf(b -> b.id == id);
            if (removed) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
            } else {
                sendError(resp, 404, "Booking not found");
            }
        } catch (NumberFormatException e) {
            sendError(resp, 400, "Invalid ID format");
        }
    }

    private boolean isValid(String authHeader) {
        // Basic admin:password
        return authHeader.equals("Basic YWRtaW46cGFzc3dvcmQ=");
    }

    private void sendError(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json");
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", msg));
    }
}