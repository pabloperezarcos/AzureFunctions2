package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure para la operación UPDATE (actualización) de un usuario
 * en la tabla "usuarios".
 *
 * Uso:
 *  - Método HTTP PUT.
 *  - El parámetro "id" (identificador del usuario) se pasa en la URL.
 *  - El cuerpo de la solicitud debe contener en formato de texto plano: "nombre,email"
 *    que corresponde a los nuevos datos del usuario.
 *
 * Ejemplo de llamada:
 *   PUT https://[TuFunctionApp].azurewebsites.net/api/actualizarusuario?id=1
 *   Body (raw, text/plain): "Nuevo Nombre,nuevoemail@example.com"
 */
public class ActualizarUsuarioFunction {

    @FunctionName("ActualizarUsuario")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req", 
                methods = { HttpMethod.PUT }, 
                authLevel = AuthorizationLevel.ANONYMOUS
            )
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para actualizar usuario.");

        // Obtener el parámetro 'id' de la URL
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es obligatorio en la URL.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        int usuarioId;
        try {
            usuarioId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' debe ser numérico.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Obtener el cuerpo de la solicitud, que debe tener el formato "nombre,email"
        String requestBody = request.getBody().orElse("").trim();
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El cuerpo de la solicitud es obligatorio y debe tener el formato 'nombre,email'.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Separar el cuerpo en 'nombre' y 'email'
        String[] parts = requestBody.split(",");
        if (parts.length < 2) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Formato incorrecto. Se espera 'nombre,email'.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }
        String nombre = parts[0].trim();
        String email = parts[1].trim();

        // Recuperar variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;
        // Intentar establecer la conexión y actualizar el usuario
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "UPDATE usuarios SET nombre = ?, email = ? WHERE id_usuario = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, email);
                ps.setInt(3, usuarioId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Usuario actualizado exitosamente\", \"id\":" + usuarioId +
                            ", \"nombre\":\"" + nombre + "\", \"email\":\"" + email + "\"}";
                } else {
                    responseMessage = "{\"error\":\"Usuario con id " + usuarioId + " no encontrado.\"}";
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                                  .body(responseMessage)
                                  .header("Content-Type", "application/json")
                                  .build();
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            responseMessage = "{\"error\":\"Error al actualizar el usuario: " + e.getMessage() + "\"}";
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
}
