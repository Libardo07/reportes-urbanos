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

    // ── Genera token y envía el enlace ───────────────────────────────────────
    public void enviarEnlaceVerificacion(String email) {
        tokenRepository.deleteByEmail(email);
        String token = UUID.randomUUID().toString();
        tokenRepository.save(new VerificacionToken(email, token));
        enviarEmailHtml(email, baseUrl + "/verificar-correo/confirmar?token=" + token);
    }

    // ── Valida el token del enlace ───────────────────────────────────────────
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

    // ── Reenvía el enlace (máx. 3 reenvíos) ─────────────────────────────────
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

    // ── Email HTML profesional ───────────────────────────────────────────────
    private void enviarEmailHtml(String destinatario, String enlace) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject("Verifica tu correo electrónico — Reportes Urbanos");
            helper.setText(buildEmailHtml(enlace), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar email de verificación", e);
        }
    }

    // Limpia token pendiente de un email (usado en cooldown de registro)
    public void limpiarPendiente(String email) {
        tokenRepository.deleteByEmail(email);
    }

        // Cuando el usuario cambia el correo: borra el token VIEJO, crea el NUEVO
    public void cambiarCorreoVerificacion(String emailAnterior, String emailNuevo) {
        tokenRepository.deleteByEmail(emailAnterior);
        enviarEnlaceVerificacion(emailNuevo);
    }

    private String buildEmailHtml(String enlace) {
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
                                box-shadow:0 2px 16px rgba(26,115,232,.10);overflow:hidden;">

                    <!-- CABECERA -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a73e8,#1557b0);
                                padding:36px 40px;text-align:center;">
                        <table cellpadding="0" cellspacing="0"
                              style="margin:0 auto 18px;">
                          <tr><td align="center" valign="middle"
                                  style="width:60px;height:60px;
                                        background:rgba(255,255,255,.15);
                                        border-radius:50%%;
                                        font-size:28px;line-height:60px;">
                            &#9993;
                          </td></tr>
                        </table>
                        <h1 style="color:#fff;font-size:22px;font-weight:700;
                                  margin:0;letter-spacing:-.3px;">
                          Verifica tu correo electrónico
                        </h1>
                      </td>
                    </tr>

                    <!-- CUERPO -->
                    <tr>
                      <td style="padding:36px 40px 28px;">
                        <p style="font-size:15px;color:#3c4043;margin:0 0 10px;">Hola,</p>
                        <p style="font-size:15px;color:#3c4043;line-height:1.7;margin:0 0 32px;">
                          Gracias por registrarte en <strong>Reportes Urbanos</strong>.
                          Haz clic en el botón de abajo para confirmar tu correo
                          electrónico y activar tu cuenta.
                        </p>

                        <!-- BOTÓN -->
                        <table cellpadding="0" cellspacing="0" width="100%%">
                          <tr><td align="center" style="padding-bottom:32px;">
                            <a href="%s"
                              style="display:inline-block;background:#1a73e8;color:#fff;
                                      font-size:15px;font-weight:600;text-decoration:none;
                                      padding:15px 40px;border-radius:8px;letter-spacing:.2px;">
                              Verificar correo electrónico
                            </a>
                          </td></tr>
                        </table>

                        <p style="font-size:12px;color:#80868b;line-height:1.6;margin:0 0 24px;">
                          Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                          <a href="%s" style="color:#1a73e8;word-break:break-all;">%s</a>
                        </p>

                        <hr style="border:none;border-top:1px solid #e8eaed;margin:0 0 24px;">

                        <p style="font-size:12px;color:#80868b;line-height:1.6;margin:0;">
                          🔒 Este enlace expira en <strong>24 horas</strong> y solo puede
                          usarse una vez. Si no creaste esta cuenta, ignora este correo.
                        </p>
                      </td>
                    </tr>

                    <!-- PIE -->
                    <tr>
                      <td style="background:#f8f9fa;padding:16px 40px;text-align:center;
                                border-top:1px solid #e8eaed;">
                        <p style="font-size:11px;color:#bdc1c6;margin:0;">
                          © 2025 Reportes Urbanos · Cartagena de Indias, Colombia
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(enlace, enlace, enlace);
    }
}