package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.PasswordResetTokenRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    public String enviarCorreoRecuperacion(String email, String baseUrl) {
        Usuario usuario = usuarioRepository.findByEmail(email);

        // Verificar que existe y que no es usuario de Google
        if (usuario == null) {
            return "Si el correo está registrado, recibirás un enlace de recuperación.";
        }
        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            return "Esta cuenta usa Google para iniciar sesión, no necesita contraseña.";
        }

        // Eliminar token anterior si existe
        tokenRepository.deleteByEmail(email);

        // Crear nuevo token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, email);
        tokenRepository.save(resetToken);

        // Enviar correo
        String enlace = baseUrl + "/recuperar-password/reset?token=" + token;
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Recuperación de contraseña - Reportes Urbanos");
        mensaje.setText("Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                + "Haz clic en el siguiente enlace para crear una nueva contraseña:\n"
                + enlace + "\n\n"
                + "Este enlace expira en 30 minutos.\n\n"
                + "Si no solicitaste esto, ignora este correo.\n\n"
                + "Reportes Urbanos - Cartagena de Indias");
        mailSender.send(mensaje);

        return "Si el correo está registrado, recibirás un enlace de recuperación.";
    }

    public String resetearPassword(String token, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            return "Las contraseñas no coinciden.";
        }
        if (nuevaPassword.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres.";
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null || resetToken.isExpirado()) {
            return "El enlace de recuperación es inválido o ha expirado.";
        }

        Usuario usuario = usuarioRepository.findByEmail(resetToken.getEmail());
        if (usuario == null) {
            return "Usuario no encontrado.";
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        tokenRepository.delete(resetToken);

        return "ok";
    }
}
