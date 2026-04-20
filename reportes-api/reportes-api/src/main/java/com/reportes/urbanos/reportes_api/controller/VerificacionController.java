package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.VerificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class VerificacionController {

    @Autowired
    private VerificacionService verificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/verificar-correo")
    public String mostrarVerificacion(HttpSession session, Model model) {
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null) return "redirect:/registro";
        model.addAttribute("email", email);
        model.addAttribute("emailOculto", ocultarEmail(email));
        return "verificar_correo";
    }

    @PostMapping("/verificar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verificarCodigo(
            @RequestParam String codigo,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null) {
            response.put("resultado", "ERROR");
            return ResponseEntity.badRequest().body(response);
        }

        String resultado = verificacionService.verificarCodigo(email, codigo);

        if (resultado.equals("OK")) {
            Usuario usuario = usuarioRepository.findByEmail(email);
            if (usuario != null) {
                usuario.setVerificado(true);
                usuarioRepository.save(usuario);
            }
            session.removeAttribute("emailPendiente");
            response.put("resultado", "OK");
        } else {
            response.put("resultado", resultado);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reenviar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reenviarCodigo(HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String email = (String) session.getAttribute("emailPendiente");
        if (email == null) {
            response.put("resultado", "ERROR");
            return ResponseEntity.badRequest().body(response);
        }
        String resultado = verificacionService.reenviarCodigo(email);
        response.put("resultado", resultado);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-correo-verificacion")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cambiarCorreo(
            @RequestParam String nuevoEmail,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String emailActual = (String) session.getAttribute("emailPendiente");
        if (emailActual == null) {
            response.put("resultado", "ERROR");
            return ResponseEntity.badRequest().body(response);
        }

        if (usuarioRepository.findByEmail(nuevoEmail) != null) {
            response.put("resultado", "YA_EXISTE");
            return ResponseEntity.ok(response);
        }

        Usuario usuario = usuarioRepository.findByEmail(emailActual);
        if (usuario != null) {
            usuario.setEmail(nuevoEmail);
            usuarioRepository.save(usuario);
        }

        session.setAttribute("emailPendiente", nuevoEmail);
        verificacionService.enviarCodigo(nuevoEmail);

        response.put("resultado", "OK");
        response.put("emailOculto", ocultarEmail(nuevoEmail));
        return ResponseEntity.ok(response);
    }

    private String ocultarEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email;
        String local = email.substring(0, at);
        String dominio = email.substring(at);
        String visible = local.substring(0, 1);
        String oculto = "*".repeat(Math.min(local.length() - 2, 6));
        String ultimo = local.substring(local.length() - 1);
        return visible + oculto + ultimo + dominio;
    }
}
