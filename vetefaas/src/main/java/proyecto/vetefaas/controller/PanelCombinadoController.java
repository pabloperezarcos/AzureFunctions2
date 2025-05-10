package proyecto.vetefaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import proyecto.vetefaas.service.AzureFunctionService;

@Controller
public class PanelCombinadoController {

    private final AzureFunctionService azureService;

    public PanelCombinadoController(AzureFunctionService azureService) {
        this.azureService = azureService;
    }

    @GetMapping("/modo/{tipo}")
    public String mostrarModo(@PathVariable String tipo, Model model) {
        model.addAttribute("modo", tipo);

        if ("graphql".equalsIgnoreCase(tipo)) {
            model.addAttribute("usuarios", azureService.obtenerUsuariosGraphql());
            model.addAttribute("roles", azureService.obtenerRolesGraphql());
        } else if ("rest".equalsIgnoreCase(tipo)) {
            model.addAttribute("usuarios", azureService.obtenerUsuariosRest());
            model.addAttribute("roles", azureService.obtenerRolesRest());
        } else {
            return "redirect:/";
        }

        return "panel_combinado";
    }
}
