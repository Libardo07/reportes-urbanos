package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.EmailValidationService;
import com.reportes.urbanos.reportes_api.service.UsuarioService;

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

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailValidationService emailValidationService;

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
            Usuario usuario = usuarioService.getUsuarioPorEmail(authentication.getName());
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

        // 1. Validar que el email es real con AbstractAPI
        String emailError = emailValidationService.validarEmail(usuario.getEmail());
        if (emailError != null) {
            model.addAttribute("error", emailError);
            return "registro";
        }

        // 2. Verificar que no esté ya registrado
        if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "registro";
        }

        // 3. Guardar usuario
        usuario.setRol(Rol.CIUDADANO);
        usuario.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);

        return "redirect:/login?registroExitoso";
    }


    

    
}