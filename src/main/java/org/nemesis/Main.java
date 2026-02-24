package org.nemesis;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // 1. Our API Endpoint
        context.addServlet(new ServletHolder(new BookingServlet()), "/bookings/*");
        context.addServlet(new ServletHolder(new HealthServlet()), "/health");

        // 2. Mock Swagger Documentation Endpoint
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/html");
                resp.getWriter().println("<html><head><link rel='stylesheet' href='https://unpkg.com/swagger-ui-dist/swagger-ui.css'></head>" +
                                         "<body><div id='swagger-ui'></div><script src='https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js'></script>" +
                                         "<script>window.onload = () => { SwaggerUIBundle({url: '/swagger.json', dom_id: '#swagger-ui'}); }</script></body></html>");
            }
        }), "/docs");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                var json = BookingOpenApiFactory.getSchema();
                resp.setContentType("application/json");
                resp.getWriter().println(json);
            }
        }), "/swagger.json");

        // Create a global error handler servlet
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // Retrieve exception info if provided by Jetty
                Object exception = req.getAttribute("jakarta.servlet.error.exception");
                String message = (exception instanceof Throwable) ?
                        ((Throwable) exception).getMessage() : "Unknown Error";

                resp.setStatus(resp.getStatus()); // Keep the original status code (404, 500, etc)
                resp.setContentType("application/json");
                resp.getWriter().write(String.format("{\"error\": \"%s\", \"status\": %d}", message, resp.getStatus()));
            }
        }), "/error");

        // 1. Add the Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutdown signal received. Stopping Jetty...");
                if (server.isRunning()) {
                    server.stop(); // This triggers a graceful shutdown
                }
                System.out.println("Jetty stopped successfully.");
            } catch (Exception e) {
                System.err.println("Error during graceful shutdown: " + e.getMessage());
            }
        }));

        // 2. Start and Join
        try {
            server.start();
            System.out.println("Server started on http://localhost:8080/docs");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}