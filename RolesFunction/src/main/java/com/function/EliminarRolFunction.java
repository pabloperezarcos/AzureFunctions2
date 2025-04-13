package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Función serverless en Azure para eliminar un rol de la tabla "ROLES".
 *
 * Uso:
 * - Método HTTP DELETE.
 * - Se debe pasar el parámetro "id" en la URL, por ejemplo:
 * https://<TuFunctionApp>.azurewebsites.net/api/eliminarrol?id=1
 *
 * La función se conecta a la base de datos utilizando las variables de entorno:
 * DB_URL, DB_USER y DB_PASSWORD.
 */
public class EliminarRolFunction {

    /**
     * Función "EliminarRol" invocable mediante una petición HTTP DELETE.
     *
     * @param request Mensaje HTTP que debe incluir el parámetro "id" en la URL.
     * @param context Permite registrar logs e información de la ejecución.
     * @return Respuesta en formato JSON indicando el resultado de la operación.
     */
    @FunctionName("EliminarRol")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando solicitud para eliminar rol.");

        // Obtener el parámetro "id" de la URL. Este parámetro corresponde al id_rol de
        // la tabla "ROLES"
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"El parámetro 'id' es obligatorio.\"}")
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

        // Recuperar las variables de entorno para la conexión a la base de datos.
        // DB_URL, DB_USER y DB_PASSWORD deben estar configuradas en la Function App.
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        String responseMessage;

        // Intentar establecer la conexión y ejecutar la sentencia DELETE
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // La tabla "ROLES" se define en mayúsculas y se indica entre comillas para
            // respetar el case sensitive.
            String sql = "DELETE FROM \"ROLES\" WHERE id_rol = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, rolId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    responseMessage = "{\"mensaje\":\"Rol eliminado exitosamente\", \"id\":" + rolId + "}";
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
            responseMessage = "{\"error\":\"Error al eliminar el rol: " + e.getMessage() + "\"}";
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseMessage)
                    .header("Content-Type", "application/json")
                    .build();
        }

        // Retornar respuesta exitosa con código HTTP 200
        return request.createResponseBuilder(HttpStatus.OK)
                .body(responseMessage)
                .header("Content-Type", "application/json")
                .build();
    }
}
