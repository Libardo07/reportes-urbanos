package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.VerificacionToken;
import com.reportes.urbanos.reportes_api.repository.VerificacionTokenRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VerificacionService {

    @Autowired
    private VerificacionTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    // Genera un token de verificación y envía el enlace al correo del usuario
    public void enviarEnlaceVerificacion(String email) {
        tokenRepository.deleteByEmail(email);
        String token = UUID.randomUUID().toString();
        tokenRepository.save(new VerificacionToken(email, token));
        enviarEmailHtml(email, baseUrl + "/verificar-correo/confirmar?token=" + token);
    }

    // Verifica si el token es válido, no ha expirado y no ha sido usado
    public String confirmarToken(String token) {
        Optional<VerificacionToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty())        return "INVALIDO";
        VerificacionToken vt = opt.get();
        if (vt.isUsado())         return "YA_USADO";
        if (vt.isExpirado()) {
            tokenRepository.delete(vt);
            return "EXPIRADO";
        }
        vt.setUsado(true);
        tokenRepository.save(vt);
        return "OK:" + vt.getEmail();
    }

    // Reenvía el enlace de verificación al usuario con un límite máximo de 3 intentos
    public String reenviarEnlace(String email) {
        VerificacionToken existing = tokenRepository.findByEmail(email);
        int reenvios = existing != null ? existing.getReenvios() : 0;
        if (reenvios >= 3) return "LIMITE";
        enviarEnlaceVerificacion(email);
        VerificacionToken nueva = tokenRepository.findByEmail(email);
        nueva.setReenvios(reenvios + 1);
        tokenRepository.save(nueva);
        return "OK";
    }

    // Envía un correo con versión HTML y versión de texto plano con el enlace para verificar la cuenta
    private void enviarEmailHtml(String destinatario, String enlace) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setFrom("serviciodereporte@gmail.com", "Reportes Urbanos");
            helper.setSubject("Verifica tu correo electrónico — Reportes Urbanos");
            helper.setText(buildEmailTexto(enlace), buildEmailHtml(enlace));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar email: " + e.getMessage() 
          + " | Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "ninguna"), e);
        }
    }

    // Elimina el token de verificación pendiente asociado a un correo si existe
    public void limpiarPendiente(String email) {
        tokenRepository.deleteByEmail(email);
    }

    // Actualiza el correo de verificación eliminando el token anterior y generando uno nuevo
    public void cambiarCorreoVerificacion(String emailAnterior, String emailNuevo) {
        tokenRepository.deleteByEmail(emailAnterior);
        enviarEnlaceVerificacion(emailNuevo);
    }

    private String buildEmailTexto(String enlace) {
        return CorreoPlantilla.verificacionTexto(enlace);
    }

    private String buildEmailHtml(String enlace) {
        return CorreoPlantilla.verificacionHtml(baseUrl, enlace);
    }
}