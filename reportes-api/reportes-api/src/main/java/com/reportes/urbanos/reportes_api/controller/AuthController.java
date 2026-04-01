package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "registroExitoso", required = false) String registroExitoso,
                                Model model,
                                Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
            if (usuario != null && usuario.getRol() == Rol.ADMIN) {
                return "redirect:/admin/inicio";
            }
            return "redirect:/usuario/inicio";
        }
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (registroExitoso != null) {
            model.addAttribute("registroExitoso", true);
        }
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
        usuario.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
        return "redirect:/login?registroExitoso";
    }


    

    
}