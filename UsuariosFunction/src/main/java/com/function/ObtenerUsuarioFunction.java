package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Función serverless en Azure para consultar un usuario y enviar evento a Event
 * Grid.
 */
public class ObtenerUsuarioFunction {

    private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://duoc-eventgrid.eastus-1.eventgrid.azure.net/api/events";
    private static final String EVENT_GRID_TOPIC_KEY = "1gI3QLmJzNponcQy1U6MGqj9FVXeKzrjqbZRbfyhFs5Gd89Woz9gJQQJ99BDACYeBjFXJ3w3AAABAZEGGB1u";

    @FunctionName("ObtenerUsuario")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para obtener usuario.");

        // Obtener el parámetro 'id' enviado en la URL
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es requerido.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        int userId;
        try {
            userId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' debe ser numérico.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Recuperar las variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String nombre = null;
        String email = null;

        // Conectar a la base de datos y ejecutar una consulta SELECT
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "SELECT nombre, email FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombre = rs.getString("nombre");
                        email = rs.getString("email");

                        // Enviar evento a Event Grid
                        sendEventToEventGrid("UsuarioConsultado", userId, nombre, email, context);
                    } else {
                        return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                                .body("{\"error\":\"Usuario con id " + userId + " no encontrado.\"}")
                                .header("Content-Type", "application/json")
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Error al obtener el usuario: " + e.getMessage() + "\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Construir la respuesta en formato JSON con los datos del usuario
        String responseBody = "{\"id\":" + userId + ", \"nombre\":\"" + nombre + "\", \"email\":\"" + email + "\"}";
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseBody)
                .header("Content-Type", "application/json")
                .build();
    }

    private void sendEventToEventGrid(String eventType, int userId, String nombre, String email,
            ExecutionContext context) {
        try {
            String eventId = UUID.randomUUID().toString();
            String eventTime = OffsetDateTime.now().toString();

            String jsonEvent = """
                    [{
                        "id": "%s",
                        "eventType": "%s",
                        "subject": "usuario/consultado",
                        "eventTime": "%s",
                        "data": {
                            "id": %d,
                            "nombre": "%s",
                            "email": "%s"
                        },
                        "dataVersion": "1.0"
                    }]
                    """.formatted(eventId, eventType, eventTime, userId, nombre, email);

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
