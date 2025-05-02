package proyecto.vetefaas.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import proyecto.vetefaas.services.UsuarioService;
import proyecto.vetefaas.dtos.UsuarioDto;

/**
 * Controlador REST encargado de manejar las solicitudes relacionadas con
 * usuarios.
 * Se encarga de recibir las solicitudes HTTP, transformar los
 * datos y enviarlos a los servicios correspondientes.
 */
@RestController
@RequestMapping("/usuario")
public class UsuarioController {
    /**
     * Servicio para gestionar la interacción con Azure Functions.
     */
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Endpoint para crear un nuevo usuario en el sistema.
     * Recibe un objeto UsuarioDto con el nombre y email del usuario
     * en formato JSON, y lo transforma para enviarlo como texto plano
     * a la función "CrearUsuario" alojada en Azure.
     */
    @PostMapping
    public ResponseEntity<String> crearUsuario(UsuarioDto usuarioDto) {
        String resultado = usuarioService.crearUsuario(usuarioDto);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para asignar un rol existente a un usuario.
     * Los parámetros userId y rolId se envían por la URL como query parameters:
     * POST /usuario/asignar-rol?userId=1&rolId=2
     * 
     * El servicio internamente invoca la función "AsignarRol" en Azure,
     * que realiza el INSERT en la tabla intermedia (usuario_roles) en Oracle.
     */
    @PostMapping("/asignar-rol")
    public ResponseEntity<String> asignarRol(@RequestParam String userId, @RequestParam String rolId) {
        String resultado = usuarioService.asignarRol(userId, rolId);
        return ResponseEntity.ok(resultado);
    }
}
