package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.service.UsuarioService;
import com.reportes.urbanos.reportes_api.service.VerificacionService;

import jakarta.servlet.http.HttpSession;

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

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VerificacionService verificacionService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "registroExitoso", required = false) String registroExitoso,
                                Model model,
                                Authentication authentication,
                                HttpSession session) {
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = usuarioService.getUsuarioPorEmail(authentication.getName());
            if (usuario != null && usuario.getRol() == Rol.ADMIN) {
                return "redirect:/admin/inicio";
            }
            return "redirect:/usuario/inicio";
        }
        if (error != null) {
            String emailParam = (String) session.getAttribute("emailNoVerificado");
            if (emailParam != null) {
                model.addAttribute("error", "Debes verificar tu correo antes de iniciar sesión.");
                model.addAttribute("emailNoVerificado", emailParam);
                session.removeAttribute("emailNoVerificado");
            } else {
                model.addAttribute("error", "Correo o contraseña incorrectos.");
            }
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
    public String registrarUsuario(@ModelAttribute Usuario usuario, Model model, HttpSession session) {
        if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
            model.addAttribute("error", "Ya existe un usuario con ese correo.");
            return "registro";
        }
        usuario.setRol(Rol.CIUDADANO);
        usuario.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);

        verificacionService.enviarCodigo(usuario.getEmail());
        session.setAttribute("emailPendiente", usuario.getEmail());

        return "redirect:/verificar-correo";
    }
}