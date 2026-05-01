package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.entity.VerificacionToken;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.repository.VerificacionTokenRepository;
import com.reportes.urbanos.reportes_api.service.VerificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class VerificacionController {

    @Autowired 
    private VerificacionService verificacionService;

    @Autowired
    private UsuarioRepository   usuarioRepository;

    @Autowired
    private VerificacionTokenRepository tokenRepository;

    // ── Pantalla "Revisa tu bandeja" ─────────────────────────────────────────
    @GetMapping("/verificar-correo")
    public String mostrarEspera(HttpSession session, Model model) {
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null) return "redirect:/registro";
        model.addAttribute("emailOculto", ocultarEmail(email));
        return "verificar_correo";
    }

    // ── Procesa el clic del enlace ───────────────────────────────────────────
    @GetMapping("/verificar-correo/confirmar")
    public String confirmarEmail(@RequestParam String token, Model model) {
        String resultado = verificacionService.confirmarToken(token);

        if (resultado.startsWith("OK:")) {
            String email = resultado.substring(3);
            Usuario usuario = usuarioRepository.findByEmail(email);
            if (usuario != null) {
                usuario.setVerificado(true);
                usuarioRepository.save(usuario);
            }

            tokenRepository.deleteByEmail(email);
            return "verificacion_exitosa";
        }

        model.addAttribute("error", resultado);
        return "verificacion_fallida";
    }

    // ── Polling: el frontend consulta si ya fue verificado ───────────────────────
    @GetMapping("/verificar-correo/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estadoVerificacion(HttpSession session) {
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null)
            return ResponseEntity.ok(Map.of("verificado", false, "sinSesion", true));

        Usuario usuario = usuarioRepository.findByEmail(email);
        boolean verificado = usuario != null && usuario.isVerificado();
        if (verificado) session.removeAttribute("emailPendiente");

        return ResponseEntity.ok(Map.of("verificado", verificado, "sinSesion", false));
    }

    // ── Reenviar enlace (desde /verificar-correo) ────────────────────────────
    @PostMapping("/reenviar-verificacion")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reenviarEnlace(HttpSession session) {
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null)
            return ResponseEntity.badRequest().body(Map.of("resultado", "ERROR"));

        String resultado = verificacionService.reenviarEnlace(email);
        return ResponseEntity.ok(Map.of("resultado", resultado));
    }

    // ── Reenviar enlace (desde /login — usuario no verificado) ───────────────
    @GetMapping("/reenviar-desde-login")
    public String reenviarDesdeLogin(@RequestParam String email, HttpSession session) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null || usuario.isVerificado())
            return "redirect:/login";

        verificacionService.enviarEnlaceVerificacion(email);
        session.setAttribute("emailPendiente", email);
        return "redirect:/verificar-correo";
    }

    // ── Cambiar correo antes de verificar ────────────────────────────────────
    @PostMapping("/cambiar-correo-verificacion")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cambiarCorreo(
            @RequestParam String nuevoEmail,
            HttpSession session) {

        String emailActual = (String) session.getAttribute("emailPendiente");
        if (emailActual == null)
            return ResponseEntity.badRequest().body(Map.of("resultado", "ERROR"));

        if (usuarioRepository.findByEmail(nuevoEmail) != null)
            return ResponseEntity.ok(Map.of("resultado", "YA_EXISTE"));

        Usuario usuario = usuarioRepository.findByEmail(emailActual);
        if (usuario != null) {
            usuario.setEmail(nuevoEmail);
            usuarioRepository.save(usuario);
        }

        session.setAttribute("emailPendiente", nuevoEmail);
        verificacionService.cambiarCorreoVerificacion(emailActual, nuevoEmail);

        return ResponseEntity.ok(Map.of(
            "resultado",    "OK",
            "emailOculto",  ocultarEmail(nuevoEmail)
        ));
    }

    // ── Utilidad ─────────────────────────────────────────────────────────────
    private String ocultarEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email;
        String local   = email.substring(0, at);
        String dominio = email.substring(at);
        String oculto  = "*".repeat(Math.min(local.length() - 2, 6));
        return local.charAt(0) + oculto + local.charAt(local.length() - 1) + dominio;
    }

    
}