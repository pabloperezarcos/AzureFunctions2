package com.function;

import java.sql.*;
import java.util.*;

/**
 * Clase encargada de realizar el CRUD contra la tabla "ROLES" en Oracle.
 */
public class RolDataFetcher {

    /**
     * Obtiene un rol por su ID (id_rol).
     */
    public Map<String, Object> obtenerRolPorId(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT rol FROM ROLES WHERE id_rol = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Retornamos un Map con 'id' y 'rol'
                        return Map.of(
                                "id", id,
                                "rol", rs.getString("rol"));
                    }
                }
            }
        }
        return null; // o podrías lanzar excepción si no se encontró
    }

    /**
     * Lista todos los roles existentes.
     */
    public List<Map<String, Object>> listarRoles() throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id_rol, rol FROM ROLES";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> roles = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id_rol");
                    String rolName = rs.getString("rol");
                    roles.add(Map.of("id", id, "rol", rolName));
                }
                return roles;
            }
        }
    }

    /**
     * Crea un nuevo rol con el nombre 'rol' en la BD.
     */
    public Map<String, Object> crearRol(String rolName) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO ROLES (rol) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, rolName);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (Statement st = conn.createStatement();
                            ResultSet rs = st.executeQuery("SELECT \"ADMIN\".\"ISEQ$$_112310\".currval FROM dual")) {
                        if (rs.next()) {
                            int idGenerado = rs.getInt(1);
                            return Map.of("id", idGenerado, "rol", rolName);
                        }
                    }
                }
            }
        }
        throw new SQLException("No se pudo crear el rol en la BD con secuencia.");
    }

    /**
     * Actualiza el nombre de un rol existente.
     */
    public Map<String, Object> actualizarRol(int id, String newRol) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "UPDATE ROLES SET rol = ? WHERE id_rol = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newRol);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return Map.of("id", id, "rol", newRol);
                }
            }
        }
        throw new SQLException("No se pudo actualizar el rol con id " + id);
    }

    /**
     * Elimina un rol por su ID.
     * Retorna true si se elimina correctamente.
     */
    public boolean eliminarRol(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM ROLES WHERE id_rol = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        }
    }

    /**
     * Helper para obtener la conexión JDBC desde las variables de entorno:
     * DB_URL, DB_USER, DB_PASSWORD.
     */
    private Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
