package proyecto.vetefaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import proyecto.vetefaas.model.Rol;
import proyecto.vetefaas.service.AzureFunctionService;

import java.util.List;

@Controller
@RequestMapping("/roles")
public class RolesRestController {

    private final AzureFunctionService azureService;

    public RolesRestController(AzureFunctionService azureService) {
        this.azureService = azureService;
    }

    // Mostrar listado de roles
    @GetMapping
    public String listarRoles(@RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        List<Rol> roles = "rest".equalsIgnoreCase(modo)
                ? azureService.obtenerRolesRest()
                : azureService.obtenerRolesGraphql();

        model.addAttribute("roles", roles);
        model.addAttribute("modo", modo);
        return "roles";
    }

    // Mostrar formulario de creación
    @GetMapping("/crear")
    public String mostrarFormularioCrear(
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        model.addAttribute("rol", new Rol());
        model.addAttribute("modo", modo);
        return "formulario_rol";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo,
            Model model) {
        Rol rol = azureService.obtenerRolPorId(id);
        model.addAttribute("rol", rol);
        model.addAttribute("modo", modo);
        return "formulario_rol";
    }

    // Procesar formulario
    @PostMapping("/guardar")
    public String guardarRol(@ModelAttribute Rol rol,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo) {
        if (rol.getId() == null) {
            azureService.crearRol(rol);
        } else {
            azureService.actualizarRol(rol);
        }
         return "redirect:/modo/" + modo;
    }

    // Eliminar rol
    @GetMapping("/eliminar/{id}")
    public String eliminarRol(@PathVariable int id,
            @RequestParam(name = "modo", required = false, defaultValue = "graphql") String modo) {
        azureService.eliminarRol(id);
         return "redirect:/modo/" + modo;
    }
}
