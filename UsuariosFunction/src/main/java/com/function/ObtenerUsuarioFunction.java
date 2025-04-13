package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure que permite obtener la información de un usuario
 * a partir de su identificador. La petición debe ser del tipo GET y debe incluir
 * el parámetro "id" en la URL, por ejemplo:
 *
 *   https://[NombreFunctionApp].azurewebsites.net/api/obtenerusuario?id=1
 *
 * La función se conecta a la base de datos Oracle utilizando las variables de
 * entorno DB_URL, DB_USER y DB_PASSWORD. Si se encuentra el usuario, devuelve
 * los datos en formato JSON; de lo contrario, retorna un error.
 */
public class ObtenerUsuarioFunction {

    @FunctionName("ObtenerUsuario")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = { HttpMethod.GET },
                authLevel = AuthorizationLevel.ANONYMOUS
            )
            HttpRequestMessage<Optional<String>> request,
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
}
