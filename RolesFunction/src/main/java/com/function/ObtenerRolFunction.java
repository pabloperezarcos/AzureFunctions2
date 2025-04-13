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
 * Función serverless en Azure que permite obtener la información de un rol
 * a partir de su identificador (id_rol) en la tabla "ROLES".
 *
 * Uso:
 * - Método HTTP GET.
 * - Se debe pasar el parámetro "id" en la URL, por ejemplo:
 * https://<NombreFunctionApp>.azurewebsites.net/api/obtenerrol?id=1
 *
 * La función utiliza las variables de entorno DB_URL, DB_USER y DB_PASSWORD
 * para
 * conectarse a la base de datos. Si se encuentra el rol, retorna un JSON con
 * el id y el nombre del rol.
 */
public class ObtenerRolFunction {

    @FunctionName("ObtenerRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para obtener rol.");

        // Obtener el parámetro 'id' enviado en la URL
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

        // Recuperar las variables de entorno para la conexión a la base de datos
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String rol = null;

        // Conectar a la base de datos y ejecutar la consulta para obtener el rol
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // La consulta busca el nombre del rol en la tabla "ROLES"
            String sql = "SELECT rol FROM \"ROLES\" WHERE id_rol = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, rolId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rol = rs.getString("rol");
                    } else {
                        return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                                .body("{\"error\":\"Rol con id " + rolId + " no encontrado.\"}")
                                .header("Content-Type", "application/json")
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            context.getLogger().severe("Error SQL: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Error al obtener el rol: " + e.getMessage() + "\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Construir la respuesta en formato JSON con los datos obtenidos
        String responseBody = "{\"id\":" + rolId + ", \"rol\":\"" + rol + "\"}";
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseBody)
                .header("Content-Type", "application/json")
                .build();
    }
}
