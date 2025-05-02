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
 * Función serverless en Azure encargada de crear un nuevo registro en la tabla
 * "ROLES" y enviar un evento a Event Grid cuando se crea correctamente.
 */
public class CrearRolFunction {

    private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://topic-cloudnative2.eastus-1.eventgrid.azure.net/api/events";
    private static final String EVENT_GRID_TOPIC_KEY = "C9MVwYnSdg8YinP6KPbsmkMjy2GsXAZCWGuu4G0gZxzomjtIX8BnJQQJ99BEACYeBjFXJ3w3AAABAZEGfSgJ";

    @FunctionName("CrearRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para crear rol.");

        // Obtener el cuerpo de la solicitud como texto plano.
        // Se espera un formato: "NombreDelRol"
        String requestBody = request.getBody().orElse("").trim();

        // Validar que se proporcione el nombre del rol
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Por favor, proporciona el nombre del rol en el cuerpo de la solicitud.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Almacenar el nombre del rol recibido
        String rol = requestBody;

        // Leer variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;

        // Intentar conectarse a la base de datos y ejecutar la sentencia de inserción
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // La tabla se denomina "ROLES" (en mayúsculas) y se coloca entre comillas
            // para preservar el case sensitive.
            String sql = "INSERT INTO \"ROLES\" (rol) VALUES (?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, rol);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Rol creado exitosamente\", \"rol\":\"" + rol + "\"}";

                    // Enviar evento a Event Grid
                    sendEventToEventGrid("RolCreado", rol, context);
                } else {
                    responseMessage = "{\"error\":\"No se pudo crear el rol.\"}";
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            responseMessage = "{\"error\":\"Error al crear el rol: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Devolver la respuesta exitosa en formato JSON
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }

    private void sendEventToEventGrid(String eventType, String rolName, ExecutionContext context) {
        try {
            String eventId = UUID.randomUUID().toString();
            String eventTime = OffsetDateTime.now().toString();

            String jsonEvent = """
                    [{
                        "id": "%s",
                        "eventType": "%s",
                        "subject": "rol/creado",
                        "eventTime": "%s",
                        "data": {
                            "rol": "%s"
                        },
                        "dataVersion": "1.0"
                    }]
                    """.formatted(eventId, eventType, eventTime, rolName);

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
