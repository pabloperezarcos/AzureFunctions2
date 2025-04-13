package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure para la operación UPDATE (actualización) de un
 * rol.
 * 
 * Uso:
 * - Método HTTP PUT.
 * - Se debe pasar el parámetro "id" en la URL, por ejemplo:
 * https://[NombreFunctionApp].azurewebsites.net/api/actualizarrol?id=1
 * - El cuerpo de la solicitud debe ser texto plano que contenga el nuevo nombre
 * del rol,
 * por ejemplo: "NuevoRol".
 *
 * La función se conecta a la base de datos utilizando las variables de entorno:
 * DB_URL, DB_USER y DB_PASSWORD.
 */
public class ActualizarRolFunction {

    @FunctionName("ActualizarRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.PUT }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para actualizar rol.");

        // Obtener el parámetro 'id' de la URL, que identifica el rol a actualizar.
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es obligatorio en la URL.\"}")
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

        // Obtener el cuerpo de la solicitud.
        // Se espera que contenga el nuevo nombre del rol en texto plano.
        String requestBody = request.getBody().orElse("").trim();
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El cuerpo de la solicitud es obligatorio y debe contener el nuevo nombre del rol.\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }
        String nuevoNombreRol = requestBody;

        // Leer variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;

        // Conectar a la base de datos y ejecutar la actualización
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Actualizar el rol en la tabla "ROLES". Se usan comillas para preservar el
            // case sensitive.
            String sql = "UPDATE \"ROLES\" SET rol = ? WHERE id_rol = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, nuevoNombreRol);
                ps.setInt(2, rolId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Rol actualizado exitosamente\", \"id\":" + rolId
                            + ", \"nuevoRol\":\"" + nuevoNombreRol + "\"}";
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
            responseMessage = "{\"error\":\"Error al actualizar el rol: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Retornar respuesta exitosa en formato JSON con HTTP 200
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }
}
