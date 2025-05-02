package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Función serverless en Azure para eliminar un rol de la tabla "ROLES" y enviar
 * un evento a Event Grid cuando se crea correctamente.
 */
public class EliminarRolFunction {

    private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://duoc-eventgrid.eastus-1.eventgrid.azure.net/api/events";
    private static final String EVENT_GRID_TOPIC_KEY = "1gI3QLmJzNponcQy1U6MGqj9FVXeKzrjqbZRbfyhFs5Gd89Woz9gJQQJ99BDACYeBjFXJ3w3AAABAZEGGB1u";

    /**
     * Función "EliminarRol" invocable mediante una petición HTTP DELETE.
     *
     * @param request Mensaje HTTP que debe incluir el parámetro "id" en la URL.
     * @param context Permite registrar logs e información de la ejecución.
     * @return Respuesta en formato JSON indicando el resultado de la operación.
     */
    @FunctionName("EliminarRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para eliminar rol.");

        // Obtener el parámetro "id" de la URL. Este parámetro corresponde al id_rol de
        // la tabla "ROLES"
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es obligatorio.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        int rolId;
        try {
            rolId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' debe ser numérico.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Recuperar las variables de entorno para la conexión a la base de datos.
        // DB_URL, DB_USER y DB_PASSWORD deben estar configuradas en la Function App.
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;

        // Intentar establecer la conexión y ejecutar la sentencia DELETE
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // La tabla "ROLES" se define en mayúsculas y se indica entre comillas para
            // respetar el case sensitive.
            String sql = "DELETE FROM \"ROLES\" WHERE id_rol = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, rolId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Rol eliminado exitosamente\", \"id\":" + rolId + "}";

                    // Enviar evento a Event Grid
                    sendEventToEventGrid("RolEliminado", rolId, context);
                } else {
                    responseMessage = "{\"error\":\"Rol con id " + rolId + " no encontrado.\"}";
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body(responseMessage)
                            .header("Content-Type", "application/json")
                            .build();
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            responseMessage = "{\"error\":\"Error al eliminar el rol: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Retornar respuesta exitosa con código HTTP 200
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }

    private void sendEventToEventGrid(String eventType, int rolId, ExecutionContext context) {
        try {
            String eventId = UUID.randomUUID().toString();
            String eventTime = OffsetDateTime.now().toString();

            String jsonEvent = """
                    [{
                        "id": "%s",
                        "eventType": "%s",
                        "subject": "rol/eliminado",
                        "eventTime": "%s",
                        "data": {
                            "id": %d
                        },
                        "dataVersion": "1.0"
                    }]
                    """.formatted(eventId, eventType, eventTime, rolId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EVENT_GRID_TOPIC_ENDPOINT))
                    .header("aeg-sas-key", EVENT_GRID_TOPIC_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonEvent))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> context.getLogger()
                            .info("Evento enviado a Event Grid: " + response.statusCode()))
                    .exceptionally(e -> {
                        context.getLogger().severe("Error al enviar evento a Event Grid: " + e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            context.getLogger().severe("Excepción al construir evento: " + e.getMessage());
        }
    }
}
