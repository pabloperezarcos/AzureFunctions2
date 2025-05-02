package proyecto.vetefaas.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import proyecto.vetefaas.model.Usuario;
import proyecto.vetefaas.service.AzureFunctionService;

@Controller
@RequestMapping("/usuarios")
public class UsuariosRestController {

    private final AzureFunctionService azureService;

    public UsuariosRestController(AzureFunctionService azureService) {
        this.azureService = azureService;
    }

    // Mostrar listado de usuarios
    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = azureService.obtenerTodosLosUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios"; // corresponde a templates/usuarios.html
    }

    // Mostrar formulario de creación
    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "formulario_usuario";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id, Model model) {
        Usuario usuario = azureService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        return "formulario_usuario";
    }

    // Procesar formulario (crear o actualizar)
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        if (usuario.getId() == null) {
            azureService.crearUsuario(usuario);
        } else {
            azureService.actualizarUsuario(usuario);
        }
        return "redirect:/usuarios";
    }

    // Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable int id) {
        azureService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}
