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

    @Autowired private UsuarioRepository            usuarioRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private JavaMailSender               mailSender;
    @Autowired private PasswordEncoder              passwordEncoder;

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
        enviarEmailHtml(email, usuario.getNombre(), enlace);

        return "Si el correo está registrado, recibirás un enlace de recuperación.";
    }

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

    // ── Email HTML profesional ───────────────────────────────────────────────
    private void enviarEmailHtml(String destinatario, String nombre, String enlace) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setFrom("serviciodereporte@gmail.com", "Reportes Urbanos");
            helper.setSubject("Recupera tu contraseña — Reportes Urbanos");
            helper.setText(buildEmailHtml(nombre, enlace), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar email de recuperación", e);
        }
    }

    private String buildEmailHtml(String nombre, String enlace) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f4f7fb;
                         font-family:'Google Sans',Roboto,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background:#f4f7fb;padding:48px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#fff;border-radius:16px;
                                box-shadow:0 2px 16px rgba(26,115,232,.10);
                                overflow:hidden;">

                    <!-- CABECERA -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a73e8,#1557b0);
                                 padding:36px 40px;text-align:center;">
                        <!-- Ícono con tabla (compatible con todos los clientes de correo) -->
                        <table cellpadding="0" cellspacing="0"
                               style="margin:0 auto 18px;">
                          <tr><td align="center" valign="middle"
                                  style="width:60px;height:60px;
                                         background:rgba(255,255,255,.15);
                                         border-radius:50%%;
                                         font-size:28px;line-height:60px;">
                            &#128274;
                          </td></tr>
                        </table>
                        <h1 style="color:#fff;font-size:22px;font-weight:700;
                                   margin:0;letter-spacing:-.3px;">
                          Recupera tu contraseña
                        </h1>
                      </td>
                    </tr>

                    <!-- CUERPO -->
                    <tr>
                      <td style="padding:36px 40px 28px;">
                        <p style="font-size:15px;color:#3c4043;margin:0 0 10px;">
                          Hola, <strong>%s</strong>
                        </p>
                        <p style="font-size:15px;color:#3c4043;line-height:1.7;
                                  margin:0 0 32px;">
                          Recibimos una solicitud para restablecer la contraseña
                          de tu cuenta en <strong>Reportes Urbanos</strong>.
                          Haz clic en el botón para crear una nueva contraseña.
                        </p>

                        <!-- BOTÓN -->
                        <table cellpadding="0" cellspacing="0" width="100%%">
                          <tr><td align="center" style="padding-bottom:32px;">
                            <a href="%s"
                               style="display:inline-block;background:#1a73e8;
                                      color:#fff;font-size:15px;font-weight:600;
                                      text-decoration:none;padding:15px 40px;
                                      border-radius:8px;letter-spacing:.2px;">
                              Restablecer contraseña
                            </a>
                          </td></tr>
                        </table>

                        <p style="font-size:12px;color:#80868b;line-height:1.6;
                                  margin:0 0 24px;">
                          Si el botón no funciona, copia y pega este enlace:<br>
                          <a href="%s"
                             style="color:#1a73e8;word-break:break-all;">%s</a>
                        </p>

                        <hr style="border:none;border-top:1px solid #e8eaed;
                                   margin:0 0 24px;">

                        <p style="font-size:12px;color:#80868b;line-height:1.6;
                                  margin:0;">
                          &#128274; Este enlace expira en <strong>30 minutos</strong>.
                          Si no solicitaste este cambio, ignora este correo —
                          tu contraseña no será modificada.
                        </p>
                      </td>
                    </tr>

                    <!-- PIE -->
                    <tr>
                      <td style="background:#f8f9fa;padding:16px 40px;
                                 text-align:center;border-top:1px solid #e8eaed;">
                        <p style="font-size:11px;color:#bdc1c6;margin:0;">
                          &copy; 2025 Reportes Urbanos &middot;
                          Cartagena de Indias, Colombia
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nombre, enlace, enlace, enlace);
    }
}