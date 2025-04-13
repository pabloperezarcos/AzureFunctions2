package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure encargada de crear un nuevo registro en la tabla
 * "ROLES".
 * 
 * Forma de uso:
 * - Se envía una petición POST con el cuerpo en texto plano que contenga
 * únicamente
 * el nombre del rol (por ejemplo: "Administrador").
 *
 * La función utiliza las variables de entorno (DB_URL, DB_USER, DB_PASSWORD)
 * para conectarse a
 * la base de datos Oracle. Se asume que la tabla "ROLES" tiene:
 * - id_rol: autogenerado
 * - rol: el nombre del rol.
 */
public class CrearRolFunction {

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
}
