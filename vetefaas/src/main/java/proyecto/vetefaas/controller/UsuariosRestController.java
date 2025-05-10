package proyecto.vetefaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import proyecto.vetefaas.model.Usuario;
import proyecto.vetefaas.service.AzureFunctionService;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuariosRestController {

    private final AzureFunctionService azureService;

    public UsuariosRestController(AzureFunctionService azureService) {
        this.azureService = azureService;
    }

    // Mostrar listado de usuarios
    @GetMapping
    public String listarUsuarios(@RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        List<Usuario> usuarios = "rest".equalsIgnoreCase(modo)
                ? azureService.obtenerUsuariosRest()
                : azureService.obtenerUsuariosGraphql();

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("modo", modo);
        return "usuarios";
    }

    // Mostrar formulario de creación
    @GetMapping("/crear")
    public String mostrarFormularioCrear(
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("modo", modo);
        return "formulario_usuario";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        Usuario usuario = azureService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("modo", modo);
        return "formulario_usuario";
    }

    // Procesar formulario
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo) {
        if (usuario.getId() == null) {
            azureService.crearUsuario(usuario);
        } else {
            azureService.actualizarUsuario(usuario);
        }
         return "redirect:/modo/" + modo;
    }

    // Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable int id,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo) {
        azureService.eliminarUsuario(id);
         return "redirect:/modo/" + modo;
    }
}
