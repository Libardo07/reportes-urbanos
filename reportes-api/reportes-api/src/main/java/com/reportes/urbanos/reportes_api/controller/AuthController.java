package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String username,
                                @RequestParam String password,
                                HttpSession session,
                                Model model) {
        Usuario usuario = usuarioRepository.findByEmail(username);

        if (usuario != null && usuario.getPassword().equals(password)) {
            session.setAttribute("usuarioLogueado", usuario);

            if (usuario.getRol() == Rol.ADMIN) {
                return "redirect:/admin/inicio";
            } else {
                return "redirect:/usuario/inicio";
            }
        }

        model.addAttribute("error", "Correo o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "registro";
        }

        usuario.setRol(Rol.CIUDADANO);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);
        return "redirect:/login?registroExitoso";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
