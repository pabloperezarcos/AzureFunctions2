package proyecto.vetefaas.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import proyecto.vetefaas.dtos.UsuarioDto;

/**
 * Servicio que orquesta la comunicación entre el BFF (VeteFaaS) y las funciones
 * serverless en Azure.
 * 
 * Provee métodos para:
 * - Crear un usuario enviando la información a la función "CrearUsuario".
 * - Asignar un rol a un usuario enviando los parámetros a la función
 * "AsignarRol".
 */
@Service
public class UsuarioService {

    /**
     * Cliente HTTP para realizar las peticiones a las funciones en Azure.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * URL de la función en Azure para crear un usuario (Función: "CrearUsuario").
     */
    private final String CREAR_USUARIO_FUNCTION_URL = "https://crearusuario.azurewebsites.net/api/crearusuario";

    /**
     * URL de la función en Azure para asignar un rol a un usuario (Función:
     * "AsignarRol").
     */
    private final String ASIGNAR_ROL_FUNCTION_URL = "https://crearusuario.azurewebsites.net/api/asignarrol";

    /**
     * Lógica para crear un usuario en el sistema a través de la función
     * "CrearUsuario" en Azure.
     * Recibe un DTO con el nombre y el email, los concatena en formato
     * "nombre,email" y envía la petición como texto plano.
     */
    public String crearUsuario(UsuarioDto usuarioDto) {
        // Convierte la información del DTO a un formato de texto simple: "nombre,email"
        String requestBody = usuarioDto.getNombre() + "," + usuarioDto.getEmail();

        // Se configura la cabecera para indicar que enviamos texto plano
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        // Se crea la entidad HTTP con el cuerpo y los encabezados
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Se hace la solicitud POST a la función "CrearUsuario" en Azure
        ResponseEntity<String> response = restTemplate.exchange(
                CREAR_USUARIO_FUNCTION_URL,
                HttpMethod.POST,
                requestEntity,
                String.class);

        // Se retorna el cuerpo de la respuesta (mensaje de éxito o error)
        return response.getBody();
    }

    /**
     * Lógica para asignar un rol a un usuario a través de la función "AsignarRol"
     * en Azure.
     * Recibe un userId y un rolId, los envía como parámetros en la URL y la
     * petición se envía como texto plano vacío.
     */
    public String asignarRol(String userId, String rolId) {
        // Construye la URL con los parámetros de consulta
        String urlConParams = ASIGNAR_ROL_FUNCTION_URL + "?userId=" + userId + "&rolId=" + rolId;

        // Se configura la cabecera para indicar que enviamos texto plano (aunque sea
        // vacío)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        // Cuerpo vacío, en este caso no enviamos nada específico
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

        // Se hace la solicitud POST a la función "AsignarRol" en Azure
        ResponseEntity<String> response = restTemplate.exchange(
                urlConParams,
                HttpMethod.POST,
                requestEntity,
                String.class);

        // Se retorna el cuerpo de la respuesta (mensaje de éxito o error)
        return response.getBody();
    }
}
