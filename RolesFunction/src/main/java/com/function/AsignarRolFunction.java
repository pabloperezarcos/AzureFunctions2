package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

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
 * Función serverless en Azure encargada de asignar un rol existente a un
 * usuario y enviar un evento a Event Grid cuando se crea correctamente.
 * Para ello, inserta un nuevo registro en la tabla intermedia "usuario_roles".
 */
public class AsignarRolFunction {

    private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://duoc-eventgrid.eastus-1.eventgrid.azure.net/api/events";
    private static final String EVENT_GRID_TOPIC_KEY = "1gI3QLmJzNponcQy1U6MGqj9FVXeKzrjqbZRbfyhFs5Gd89Woz9gJQQJ99BDACYeBjFXJ3w3AAABAZEGGB1u";

    /**
     * Nombre de la función en Azure: "AsignarRol".
     * Se invoca con un método POST.
     */
    @FunctionName("AsignarRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para asignar rol.");

        // Obtener parámetros de consulta
        String userId = request.getQueryParameters().get("userId");
        String rolId = request.getQueryParameters().get("rolId");

        // Si no vienen en la query, intentar obtenerlos del cuerpo
        // (texto:"userId,rolId")
        Optional<String> bodyOptional = request.getBody();
        if ((userId == null || rolId == null) && bodyOptional.isPresent()) {
            String body = bodyOptional.get().trim();
            // Se asume un formato simple: "userId,rolId" (por ejemplo, "2,1")
            String[] parts = body.split(",");
            if (parts.length >= 2) {
                if (userId == null || userId.isEmpty()) {
                    userId = parts[0].trim();
                }
                if (rolId == null || rolId.isEmpty()) {
                    rolId = parts[1].trim();
                }
            }
        }

        // Validar la existencia de userId y rolId
        if (userId == null || rolId == null || userId.isEmpty() || rolId.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Por favor, proporciona 'userId' y 'rolId' en la consulta o en el cuerpo.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Obtener variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Insertar en la tabla 'usuario_roles'
            String sql = "INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (?, ?)";

            int userIdInt = Integer.parseInt(userId);
            int rolIdInt = Integer.parseInt(rolId);

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(userId));
                ps.setInt(2, Integer.parseInt(rolId));

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    // Construye mensaje de éxito en formato JSON
                    responseMessage = "{\"mensaje\":\"Rol asignado exitosamente\", \"userId\":\"" + userId
                            + "\", \"rolId\":\"" + rolId + "\"}";

                    // Enviar evento a Event Grid
                    sendEventToEventGrid("RolAsignado", userIdInt, rolIdInt, context);
                } else {
                    responseMessage = "{\"error\":\"No se pudo asignar el rol.\"}";
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            responseMessage = "{\"error\":\"Error al asignar el rol: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        } catch (NumberFormatException nfe) {
            // Maneja el caso en que userId o rolId no sean valores numéricos
            responseMessage = "{\"error\":\"Los parámetros 'userId' y 'rolId' deben ser numéricos.\"}";
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Retorna una respuesta exitosa en formato JSON
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }

    private void sendEventToEventGrid(String eventType, int userId, int rolId, ExecutionContext context) {
        try {
            String eventId = UUID.randomUUID().toString();
            String eventTime = OffsetDateTime.now().toString();

            String jsonEvent = """
                    [{
                        "id": "%s",
                        "eventType": "%s",
                        "subject": "rol/asignado",
                        "eventTime": "%s",
                        "data": {
                            "usuarioId": %d,
                            "rolId": %d
                        },
                        "dataVersion": "1.0"
                    }]
                    """.formatted(eventId, eventType, eventTime, userId, rolId);

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
