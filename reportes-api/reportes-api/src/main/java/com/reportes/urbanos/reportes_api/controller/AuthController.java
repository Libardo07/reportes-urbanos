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
import java.time.temporal.ChronoUnit;

@Controller
public class AuthController {

    @Autowired private UsuarioRepository  usuarioRepository;
    @Autowired private PasswordEncoder    passwordEncoder;
    @Autowired private UsuarioService     usuarioService;
    @Autowired private VerificacionService verificacionService;

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/login")
    public String mostrarLogin(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String registroExitoso,
            Model model,
            Authentication authentication,
            HttpSession session) {

        if (authentication != null && authentication.isAuthenticated()) {
            Usuario u = usuarioService.getUsuarioPorEmail(authentication.getName());
            return (u != null && u.getRol() == Rol.ADMIN)
                ? "redirect:/admin/inicio" : "redirect:/usuario/inicio";
        }
        if (error != null) {
            String emailNoVerificado = (String) session.getAttribute("emailNoVerificado");
            if (emailNoVerificado != null) {
                model.addAttribute("error", "Debes verificar tu correo antes de iniciar sesión.");
                model.addAttribute("emailNoVerificado", emailNoVerificado);
                session.removeAttribute("emailNoVerificado");
            } else {
                model.addAttribute("error", "Correo o contraseña incorrectos.");
            }
        }
        if (registroExitoso != null) model.addAttribute("registroExitoso", true);
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(
            @ModelAttribute Usuario usuario, Model model, HttpSession session) {

        Usuario existente = usuarioRepository.findByEmail(usuario.getEmail());

        if (existente != null) {
            if (existente.isVerificado()) {
                model.addAttribute("error", "Ya existe una cuenta con ese correo electrónico.");
                return "registro";
            }

            // Usuario pendiente de verificación — aplicar cooldown de 60 s
            LocalDateTime ahora     = LocalDateTime.now(ZoneId.of("America/Bogota"));
            long segundosTranscurridos =
                ChronoUnit.SECONDS.between(existente.getFechaCreacion(), ahora);

            if (segundosTranscurridos < 60) {
                long restantes = 60 - segundosTranscurridos;
                model.addAttribute("error",
                    "Ya hay un registro pendiente con este correo. " +
                    "Por seguridad, espera " + restantes +
                    " segundo" + (restantes == 1 ? "" : "s") +
                    " o verifica el enlace que ya te enviamos.");
                return "registro";
            }

            // Cooldown superado — limpiar registro anterior
            verificacionService.limpiarPendiente(existente.getEmail());
            usuarioRepository.delete(existente);
        }

        usuario.setRol(Rol.CIUDADANO);
        usuario.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setVerificado(false);
        usuarioRepository.save(usuario);

        verificacionService.enviarEnlaceVerificacion(usuario.getEmail());
        session.setAttribute("emailPendiente", usuario.getEmail());

        return "redirect:/verificar-correo";
    }
}