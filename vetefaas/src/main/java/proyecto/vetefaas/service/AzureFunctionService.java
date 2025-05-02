package proyecto.vetefaas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import proyecto.vetefaas.model.Usuario;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AzureFunctionService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Cambia estas URLs por las reales de tus Azure Functions
    private final String BASE_URL = "https://usuariosfunction.azurewebsites.net/api";
    private final String CREAR_USUARIO_URL = BASE_URL + "/crearusuario";
    private final String ACTUALIZAR_USUARIO_URL = BASE_URL + "/actualizarusuario?id={id}";
    private final String ELIMINAR_USUARIO_URL = BASE_URL + "/eliminarusuario?id={id}";
    private final String OBTENER_USUARIO_URL = BASE_URL + "/obtenerusuario?id={id}";

    // GraphQL URL (configurada desde application.properties)
    @Value("${azure.functions.graphql-url}")
    private String graphqlUrl;

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

    @SuppressWarnings("unchecked")
    public List<Usuario> obtenerTodosLosUsuarios() {
        String graphqlPayload = """
                {
                  "query": "query { usuarios { id nombre email } }"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(graphqlPayload, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(graphqlUrl, entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        Map<String, Object> body = response.getBody();
        if (response.getStatusCode() != HttpStatus.OK || body == null) {
            return List.of();
        }

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

}