package org.nemesis;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;

import io.swagger.v3.core.util.Json;

import java.util.List;

public class BookingOpenApiFactory {

    public static OpenAPI create() {

        // --- Booking Schema ---
        Schema<?> bookingSchema = new ObjectSchema()
                .addProperty("id", new IntegerSchema().example(1))
                .addProperty("guestName", new StringSchema().example("John Wick"))
                .addProperty("roomType", new StringSchema().example("Continental Suite"));

        // --- Error Schema ---
        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("error", new StringSchema().example("Booking not found"));

        Components components = new Components()
                .addSchemas("Booking", bookingSchema)
                .addSchemas("Error", errorSchema)
                .addSecuritySchemes("basicAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                );

        Operation getAll = new Operation()
                .summary("List all bookings")
                .responses(new ApiResponses()
                        .addApiResponse("200",
                                new ApiResponse()
                                        .description("List of bookings")
                                        .content(new Content().addMediaType(
                                                "application/json",
                                                new MediaType().schema(
                                                        new ArraySchema()
                                                                .items(new Schema<>().$ref("#/components/schemas/Booking"))
                                                )
                                        ))
                        )
                );

        Operation getById = new Operation()
                .summary("Get booking by ID")
                .addParametersItem(new Parameter()
                        .name("id")
                        .in("path")
                        .required(true)
                        .schema(new IntegerSchema()))
                .responses(new ApiResponses()
                        .addApiResponse("200",
                                jsonResponse("Booking found",
                                        "#/components/schemas/Booking"))
                        .addApiResponse("400",
                                jsonResponse("Invalid ID format",
                                        "#/components/schemas/Error"))
                        .addApiResponse("404",
                                jsonResponse("Booking not found",
                                        "#/components/schemas/Error"))
                );

        Operation create = new Operation()
                .summary("Create booking")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType(
                                "application/json",
                                new MediaType().schema(
                                        new Schema<>().$ref("#/components/schemas/Booking")
                                )
                        ))
                )
                .responses(new ApiResponses()
                        .addApiResponse("201",
                                jsonResponse("Booking created",
                                        "#/components/schemas/Booking"))
                        .addApiResponse("400",
                                jsonResponse("Malformed JSON request",
                                        "#/components/schemas/Error"))
                );

        Operation delete = new Operation()
                .summary("Delete booking")
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .addParametersItem(new Parameter()
                        .name("id")
                        .in("path")
                        .required(true)
                        .schema(new IntegerSchema()))
                .responses(new ApiResponses()
                        .addApiResponse("204",
                                new ApiResponse().description("Booking deleted"))
                        .addApiResponse("400",
                                jsonResponse("Invalid ID format",
                                        "#/components/schemas/Error"))
                        .addApiResponse("401",
                                jsonResponse("Unauthorized",
                                        "#/components/schemas/Error"))
                        .addApiResponse("404",
                                jsonResponse("Booking not found",
                                        "#/components/schemas/Error"))
                        .addApiResponse("405",
                                jsonResponse("Method not allowed",
                                        "#/components/schemas/Error"))
                );



        // --- Paths ---
        Paths paths = new Paths()
                .addPathItem("/bookings",
                        new PathItem()
                                .get(getAll)
                                .post(create)
                )
                .addPathItem("/bookings/{id}",
                        new PathItem()
                                .get(getById)
                                .delete(delete)
                );

        return new OpenAPI()
                .info(new Info()
                        .title("Booking API")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("http://localhost:8080")))
                .components(components)
                .paths(paths);
    }

    private static ApiResponse jsonResponse(String description, String schemaRef) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(
                                new Schema<>().$ref(schemaRef)
                        )
                ));
    }

    private static ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(
                                new Schema<>().$ref("#/components/schemas/Error")
                        )
                ));
    }

    public static String getSchema()  {
        OpenAPI openAPI = create();
        return Json.pretty(openAPI);
    }
}