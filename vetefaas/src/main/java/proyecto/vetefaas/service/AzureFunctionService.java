package proyecto.vetefaas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import proyecto.vetefaas.model.Usuario;
import proyecto.vetefaas.model.Rol;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AzureFunctionService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ------------------ REST Usuarios ------------------
    private final String USUARIOS_REST = "https://usuariosfunction.azurewebsites.net/api";
    private final String CREAR_USUARIO_URL = USUARIOS_REST + "/crearusuario";
    private final String ACTUALIZAR_USUARIO_URL = USUARIOS_REST + "/actualizarusuario?id={id}";
    private final String ELIMINAR_USUARIO_URL = USUARIOS_REST + "/eliminarusuario?id={id}";
    private final String OBTENER_USUARIO_URL = USUARIOS_REST + "/obtenerusuario?id={id}";

    // ------------------ REST Roles ------------------
    private final String ROLES_REST = "https://rolesfunction.azurewebsites.net/api";
    private final String CREAR_ROL_URL = ROLES_REST + "/crearrol";
    private final String ACTUALIZAR_ROL_URL = ROLES_REST + "/actualizarrol?id={id}";
    private final String ELIMINAR_ROL_URL = ROLES_REST + "/eliminarrol?id={id}";
    private final String OBTENER_ROL_URL = ROLES_REST + "/obtenerrol?id={id}";

    // ------------------ GraphQL ------------------
    @Value("${azure.functions.graphql-url-usuarios}")
    private String graphqlUsuariosUrl;

    @Value("${azure.functions.graphql-url-roles}")
    private String graphqlRolesUrl;

    // ------------------ CRUD Usuarios REST ------------------
    public void crearUsuario(Usuario usuario) {
        String body = usuario.getNombre() + "," + usuario.getEmail();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(CREAR_USUARIO_URL, entity, String.class);
    }

    public void actualizarUsuario(Usuario usuario) {
        String body = usuario.getNombre() + "," + usuario.getEmail();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(ACTUALIZAR_USUARIO_URL, HttpMethod.PUT, entity, String.class, usuario.getId());
    }

    public void eliminarUsuario(int id) {
        restTemplate.exchange(ELIMINAR_USUARIO_URL, HttpMethod.DELETE, null, String.class, id);
    }

    public Usuario obtenerUsuarioPorId(int id) {
        return restTemplate.getForObject(OBTENER_USUARIO_URL, Usuario.class, id);
    }

    public List<Usuario> obtenerTodosLosUsuarios() {
        return obtenerUsuariosGraphql();
    }

    // ------------------ CRUD Usuarios GraphQL ------------------
    public List<Usuario> obtenerUsuariosGraphql() {
        String graphqlPayload = """
                {
                  "query": "query { usuarios { id nombre email } }"
                }
                """;

        return ejecutarGraphqlUsuarios(graphqlPayload);
    }

    // ------------------ CRUD Usuarios REST (GET All) ------------------
    public List<Usuario> obtenerUsuariosRest() {
        return obtenerUsuariosGraphql();
    }

    // ------------------ CRUD Roles GraphQL ------------------
    public List<Rol> obtenerRolesGraphql() {
        String graphqlPayload = """
                {
                  "query": "query { roles { id rol } }"
                }
                """;

        return ejecutarGraphqlRoles(graphqlPayload);
    }

    // ------------------ CRUD Roles REST ------------------
    public List<Rol> obtenerRolesRest() {
        return obtenerRolesGraphql();
    }

    public void crearRol(Rol rol) {
        String body = rol.getRol();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(CREAR_ROL_URL, entity, String.class);
    }

    public void actualizarRol(Rol rol) {
        String body = rol.getRol();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(ACTUALIZAR_ROL_URL, HttpMethod.PUT, entity, String.class, rol.getId());
    }

    public void eliminarRol(int id) {
        restTemplate.exchange(ELIMINAR_ROL_URL, HttpMethod.DELETE, null, String.class, id);
    }

    public Rol obtenerRolPorId(int id) {
        return restTemplate.getForObject(OBTENER_ROL_URL, Rol.class, id);
    }

    // ------------------ Helpers privados ------------------
    @SuppressWarnings("unchecked")
    private List<Usuario> ejecutarGraphqlUsuarios(String graphqlPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(graphqlPayload, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(graphqlUsuariosUrl, entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        Map<String, Object> body = response.getBody();
        System.out.println("Respuesta completa del GraphQL: " + body);
        if (response.getStatusCode() != HttpStatus.OK || body == null)
            return List.of();

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("usuarios");

        return users.stream()
                .map(u -> {
                    Object idRaw = u.get("id");
                    int id = (idRaw instanceof Number)
                            ? ((Number) idRaw).intValue()
                            : Integer.parseInt(idRaw.toString());

                    return new Usuario(
                            id,
                            (String) u.get("nombre"),
                            (String) u.get("email"));
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Rol> ejecutarGraphqlRoles(String graphqlPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(graphqlPayload, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(graphqlRolesUrl, entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        Map<String, Object> body = response.getBody();
        if (response.getStatusCode() != HttpStatus.OK || body == null)
            return List.of();

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> roles = (List<Map<String, Object>>) data.get("roles");

        return roles.stream()
                .map(r -> {
                    Object idRaw = r.get("id");
                    int id = (idRaw instanceof Number)
                            ? ((Number) idRaw).intValue()
                            : Integer.parseInt(idRaw.toString());

                    return new Rol(
                            id,
                            (String) r.get("rol"));
                })
                .collect(Collectors.toList());
    }
}
