package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.PasswordResetTokenRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired 
    private UsuarioRepository usuarioRepository;

    @Autowired 
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired 
    private JavaMailSender mailSender;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    // Genera y envía un enlace para recuperar la contraseña del usuario
    public String enviarCorreoRecuperacion(String email, String baseUrl) {
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null)
            return "Si el correo está registrado, recibirás un enlace de recuperación.";

        if (usuario.getPassword() == null || usuario.getPassword().isEmpty())
            return "Esta cuenta usa Google para iniciar sesión, no necesita contraseña.";

        tokenRepository.deleteByEmail(email);

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, email));

        String enlace = baseUrl + "/recuperar-password/reset?token=" + token;
        enviarEmailHtml(email, usuario.getNombre(), enlace, baseUrl);

        return "Si el correo está registrado, recibirás un enlace de recuperación.";
    }

    // Verifica el token y actualiza la contraseña del usuario
    public String resetearPassword(String token, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword))
            return "Las contraseñas no coinciden.";
        if (nuevaPassword.length() < 6)
            return "La contraseña debe tener al menos 6 caracteres.";

        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null || resetToken.isExpirado())
            return "El enlace de recuperación es inválido o ha expirado.";

        Usuario usuario = usuarioRepository.findByEmail(resetToken.getEmail());
        if (usuario == null) return "Usuario no encontrado.";

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        tokenRepository.delete(resetToken);
        return "ok";
    }

    // Envía el correo (versión HTML y versión de texto) con el enlace para recuperar la contraseña.
    // El diseño del correo vive en CorreoPlantilla.
    private void enviarEmailHtml(String destinatario, String nombre, String enlace, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setFrom("serviciodereporte@gmail.com", "Reportes Urbanos");
            helper.setSubject("Recupera tu contraseña — Reportes Urbanos");
            helper.setText(
                CorreoPlantilla.recuperacionTexto(nombre, enlace),
                CorreoPlantilla.recuperacionHtml(baseUrl, nombre, enlace));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar email de recuperación: " + e.getMessage(), e);
        }
    }
}