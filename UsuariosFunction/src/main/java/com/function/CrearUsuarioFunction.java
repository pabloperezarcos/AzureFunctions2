package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.util.Optional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Función serverless en Azure encargada de crear un nuevo usuario,
 * asignarle un rol por defecto y enviar un evento a Event Grid.
 */
public class CrearUsuarioFunction {

        private static final String EVENT_GRID_TOPIC_ENDPOINT = "https://topic-cloudnative2.eastus-1.eventgrid.azure.net/api/events";
        private static final String EVENT_GRID_TOPIC_KEY = "C9MVwYnSdg8YinP6KPbsmkMjy2GsXAZCWGuu4G0gZxzomjtIX8BnJQQJ99BEACYeBjFXJ3w3AAABAZEGfSgJ";

        @FunctionName("CrearUsuario")
        public HttpResponseMessage run(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                context.getLogger().info("Procesando solicitud para crear usuario.");

                String requestBody = request.getBody().orElse("").trim();
                if (requestBody.isEmpty()) {
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                        .body("{\"error\":\"Proporciona los datos del usuario en formato 'nombre,email'.\"}")
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                String[] parts = requestBody.split(",");
                if (parts.length < 2) {
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                        .body("{\"error\":\"Formato incorrecto. Se espera 'nombre,email'.\"}")
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                String nombre = parts[0].trim();
                String email = parts[1].trim();

                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");

                String responseMessage;
                int rolPorDefecto = 3; // 👈 cambia este valor si el rol por defecto es otro

                try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                        // 1. Insertar usuario
                        String sql = "INSERT INTO usuarios (nombre, email) VALUES (?, ?)";
                        try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, nombre);
                                ps.setString(2, email);
                                ps.executeUpdate();
                        }

                        // Luego obtén el último usuario
                        int idUsuario = -1;
                        String sqlLastId = "SELECT id_usuario FROM usuarios WHERE email = ? ORDER BY id_usuario DESC FETCH FIRST 1 ROWS ONLY";
                        try (PreparedStatement psLast = connection.prepareStatement(sqlLastId)) {
                                psLast.setString(1, email);
                                try (ResultSet rs = psLast.executeQuery()) {
                                        if (rs.next()) {
                                                idUsuario = rs.getInt("id_usuario");
                                        }
                                }
                        }

                        if (idUsuario != -1) {
                                // ✅ 2. Asignar rol por defecto
                                String sqlAsignarRol = "INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (?, ?)";
                                try (PreparedStatement psRol = connection.prepareStatement(sqlAsignarRol)) {
                                        psRol.setInt(1, idUsuario);
                                        psRol.setInt(2, rolPorDefecto);
                                        psRol.executeUpdate();
                                }

                                // ✅ 3. Enviar evento
                                sendEventToEventGrid(idUsuario, context);

                                responseMessage = "{\"mensaje\":\"Usuario creado y rol asignado exitosamente\", \"idUsuario\":"
                                                + idUsuario + "}";
                        } else {
                                responseMessage = "{\"error\":\"No se pudo obtener el ID del nuevo usuario.\"}";
                        }

                } catch (SQLException e) {
                        context.getLogger().severe("Error SQL: " + e.getMessage());
                        responseMessage = "{\"error\":\"Error al crear el usuario: " + e.getMessage() + "\"}";
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(responseMessage)
                                        .header("Content-Type", "application/json")
                                        .build();
                }

                return request.createResponseBuilder(HttpStatus.OK)
                                .body(responseMessage)
                                .header("Content-Type", "application/json")
                                .build();
        }

        private void sendEventToEventGrid(int idUsuario, ExecutionContext context) {
                try {
                        String eventId = UUID.randomUUID().toString();
                        String eventTime = OffsetDateTime.now().toString();

                        String jsonEvent = """
                                        [{
                                            "id": "%s",
                                            "eventType": "UsuarioCreado",
                                            "subject": "usuario/creado",
                                            "eventTime": "%s",
                                            "data": {
                                                "idUsuario": %d
                                            },
                                            "dataVersion": "1.0"
                                        }]
                                        """.formatted(eventId, eventTime, idUsuario);

                        HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(EVENT_GRID_TOPIC_ENDPOINT))
                                        .header("aeg-sas-key", EVENT_GRID_TOPIC_KEY)
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(jsonEvent))
                                        .build();

                        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                        .thenAccept(response -> context.getLogger()
                                                        .info("Evento UsuarioCreado enviado a Event Grid: "
                                                                        + response.statusCode()))
                                        .exceptionally(e -> {
                                                context.getLogger().severe("Error al enviar evento: " + e.getMessage());
                                                return null;
                                        });

                } catch (Exception e) {
                        context.getLogger().severe("Excepción al construir evento: " + e.getMessage());
                }
        }
}