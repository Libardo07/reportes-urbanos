package com.reportes.urbanos.reportes_api.service;


import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.PasswordResetTokenRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailVerificationService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreoVerificacion(String email, String nombre, String baseUrl) {
        // Eliminar token anterior si existe
        tokenRepository.deleteByEmailAndTipo(email, "EMAIL_VERIFICATION");

        // Crear nuevo token (24 horas)
        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, email, "EMAIL_VERIFICATION"));

        // Enviar correo
        String enlace = baseUrl + "/verificar-email?token=" + token;
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Verifica tu correo - Reportes Urbanos");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Gracias por registrarte en Reportes Urbanos.\n\n"
                + "Por favor verifica tu correo haciendo clic en el siguiente enlace:\n"
                + enlace + "\n\n"
                + "Este enlace expira en 24 horas.\n\n"
                + "Si no creaste esta cuenta, ignora este correo.\n\n"
                + "Reportes Urbanos - Cartagena de Indias");
        mailSender.send(mensaje);
    }

    public boolean verificarToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null || resetToken.isExpirado()
                || !"EMAIL_VERIFICATION".equals(resetToken.getTipo())) {
            return false;
        }

        Usuario usuario = usuarioRepository.findByEmail(resetToken.getEmail());
        if (usuario == null) return false;

        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
        tokenRepository.delete(resetToken);

        return true;
    }
}
