package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure para la operación DELETE (eliminar) de un usuario
 * en la tabla "usuarios".
 *
 * Uso:
 * - Método HTTP DELETE.
 * - Se debe pasar el parámetro "id" en la URL, por ejemplo:
 * https://[NombreFunctionApp].azurewebsites.net/api/eliminarusuario?id=1
 *
 * La función se conecta a la base de datos utilizando las variables de entorno:
 * DB_URL, DB_USER y DB_PASSWORD.
 */
public class EliminarUsuarioFunction {

    @FunctionName("EliminarUsuario")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para eliminar usuario.");

        // Obtener el parámetro 'id' de la URL
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es obligatorio en la URL.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        int usuarioId;
        try {
            usuarioId = Integer.parseInt(idParam.trim());
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

        String responseMessage;
        // Conectar a la base de datos y ejecutar el DELETE
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, usuarioId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Usuario eliminado exitosamente\", \"id\":" + usuarioId + "}";
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
            responseMessage = "{\"error\":\"Error al eliminar el usuario: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Retornar respuesta exitosa con HTTP 200
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }
}
