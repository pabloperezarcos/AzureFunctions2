package com.function;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Clase encargada de los data fetchers (resolvers) para la entidad Usuario.
 * Aquí se implementan los métodos que conectan con la base de datos Oracle,
 * realizando consultas JDBC para CRUD.
 */
public class UsuarioDataFetcher {

    /**
     * Obtiene un usuario por su ID, retornando un Map con "id", "nombre" y "email".
     */
    public Map<String, Object> obtenerUsuarioPorId(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT nombre, email FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("id", id);
                        usuario.put("nombre", rs.getString("nombre"));
                        usuario.put("email", rs.getString("email"));
                        return usuario;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lista todos los usuarios (demo: sin paginación).
     */
    public List<Map<String, Object>> listarUsuarios() throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT id_usuario, nombre, email FROM usuarios";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> usuarios = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id", rs.getInt("id_usuario"));
                    usuario.put("nombre", rs.getString("nombre"));
                    usuario.put("email", rs.getString("email"));
                    usuarios.add(usuario);
                }
                return usuarios;
            }
        }
    }

    /**
     * Crea un usuario insertando (nombre, email) en la BD.
     */
    public Map<String, Object> crearUsuario(String nombre, String email) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO usuarios (nombre, email) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, email);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int id = keys.getInt(1);
                            Map<String, Object> user = new HashMap<>();
                            user.put("id", id);
                            user.put("nombre", nombre);
                            user.put("email", email);
                            return user;
                        }
                    }
                }
            }
        }
        throw new SQLException("No se pudo crear el usuario en la BD.");
    }

    /**
     * Actualiza un usuario existente.
     */
    public Map<String, Object> actualizarUsuario(int id, String nombre, String email) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "UPDATE usuarios SET nombre = ?, email = ? WHERE id_usuario = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setString(2, email);
                ps.setInt(3, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", id);
                    user.put("nombre", nombre);
                    user.put("email", email);
                    return user;
                }
            }
        }
        throw new SQLException("No se pudo actualizar el usuario con id " + id);
    }

    /**
     * Elimina un usuario por su ID. Retorna true si se eliminó con éxito.
     */
    public boolean eliminarUsuario(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        }
    }

    /**
     * Helper para obtener la conexión JDBC usando variables de entorno
     * DB_URL, DB_USER, DB_PASSWORD.
     */
    private Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
