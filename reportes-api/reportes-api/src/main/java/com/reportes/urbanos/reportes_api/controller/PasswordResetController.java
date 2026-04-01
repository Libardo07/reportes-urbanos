package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.config.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/recuperar-password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping
    public String mostrarFormularioRecuperar() {
        return "recuperar_password";
    }

    @PostMapping
    public String procesarRecuperar(@RequestParam String email,
                                    HttpServletRequest request,
                                    Model model) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String mensaje = passwordResetService.enviarCorreoRecuperacion(email, baseUrl);
        model.addAttribute("mensaje", mensaje);
        return "recuperar_password";
    }

    @GetMapping("/reset")
    public String mostrarFormularioReset(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset_password";
    }

    @PostMapping("/reset")
    public String procesarReset(@RequestParam String token,
                                @RequestParam String nuevaPassword,
                                @RequestParam String confirmarPassword,
                                Model model) {
        String resultado = passwordResetService.resetearPassword(token, nuevaPassword, confirmarPassword);
        if (resultado.equals("ok")) {
            return "redirect:/login?passwordReseteado";
        }
        model.addAttribute("error", resultado);
        model.addAttribute("token", token);
        return "reset_password";
    }
}
