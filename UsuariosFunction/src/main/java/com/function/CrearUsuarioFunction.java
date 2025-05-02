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
 * Función serverless en Azure encargada de crear un nuevo usuario
 * y enviar un evento a Event Grid.
 */
public class CrearUsuarioFunction {

        private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://topic-cloudnative2.eastus-1.eventgrid.azure.net/api/events";
        private static final String EVENT_GRID_TOPIC_KEY = "C9MVwYnSdg8YinP6KPbsmkMjy2GsXAZCWGuu4G0gZxzomjtIX8BnJQQJ99BEACYeBjFXJ3w3AAABAZEGfSgJ";

        /**
         * Nombre de la función en Azure: "CrearUsuario".
         * Invocable por método POST.
         */
        @FunctionName("CrearUsuario")
        public HttpResponseMessage run(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                context.getLogger().info("Procesando solicitud para crear usuario.");

                // Obtener el cuerpo de la solicitud como texto plano.
                // Se espera un formato: "nombre,email".
                String requestBody = request.getBody().orElse("").trim();

                // Validar que el cuerpo no esté vacío
                if (requestBody.isEmpty()) {
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                        .body("{\"error\":\"Por favor, proporciona los datos del usuario en el cuerpo de la solicitud en formato 'nombre,email'.\"}")
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                // Separar los datos por coma
                String[] parts = requestBody.split(",");
                if (parts.length < 2) {
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                        .body("{\"error\":\"Formato incorrecto. Se espera 'nombre,email'.\"}")
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                // Obtener nombre y email del array
                String nombre = parts[0].trim();
                String email = parts[1].trim();

                // Leer variables de entorno para la conexión a la DB
                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");

                String responseMessage;

                // Manejo de la conexión a la base de datos con JDBC
                try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                        String sql = "INSERT INTO usuarios (nombre, email) VALUES (?, ?)";
                        try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, nombre);
                                ps.setString(2, email);
                                int rowsAffected = ps.executeUpdate();
                                if (rowsAffected > 0) {
                                        // Construir respuesta de éxito
                                        responseMessage = "{\"mensaje\":\"Usuario creado exitosamente\", \"nombre\":\""
                                                        + nombre + "\", \"email\":\"" + email + "\"}";

                                        // Enviar evento a Event Grid
                                        sendEventToEventGrid("UsuarioCreado", nombre, email, context);
                                } else {
                                        responseMessage = "{\"error\":\"No se pudo crear el usuario.\"}";
                                }
                        }
                } catch (SQLException e) {
                        // Manejo de errores SQL
                        context.getLogger().severe("Error SQL: " + e.getMessage());
                        responseMessage = "{\"error\":\"Error al crear el usuario: " + e.getMessage() + "\"}";
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(responseMessage)
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                // Retornar respuesta exitosa
                return request.createResponseBuilder(HttpStatus.OK)
                                .body(responseMessage)
                                .header("Content-Type", "application/json")
                                .build();
        }

        private void sendEventToEventGrid(String eventType, String nombre, String email, ExecutionContext context) {
                try {
                        String eventId = UUID.randomUUID().toString();
                        String eventTime = OffsetDateTime.now().toString();

                        String jsonEvent = """
                                        [{
                                            "id": "%s",
                                            "eventType": "%s",
                                            "subject": "usuario/creado",
                                            "eventTime": "%s",
                                            "data": {
                                                "nombre": "%s",
                                                "email": "%s"
                                            },
                                            "dataVersion": "1.0"
                                        }]
                                        """.formatted(eventId, eventType, eventTime, nombre, email);

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
                                                context.getLogger().severe("Error al enviar evento a Event Grid: "
                                                                + e.getMessage());
                                                return null;
                                        });

                } catch (Exception e) {
                        context.getLogger().severe("Excepción al construir evento: " + e.getMessage());
                }
        }
}
