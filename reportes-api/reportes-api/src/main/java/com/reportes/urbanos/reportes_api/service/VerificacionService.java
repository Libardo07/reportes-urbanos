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

    // Construye la versión de texto plano del correo (para clientes que no muestran HTML)
    private String buildEmailTexto(String enlace) {
        return """
            Reportes Urbanos

            Confirma tu correo para activar tu cuenta.

            Gracias por unirte. Abre este enlace para verificar tu correo electrónico:
            %s

            El enlace expira en 3 minutos y solo puede usarse una vez.
            Si no creaste esta cuenta, ignora este correo.

            Reportes Urbanos · Cartagena de Indias, Colombia
            """.formatted(enlace);
    }

    // Construye el contenido HTML del correo de verificación de cuenta.
    // Usa tablas y estilos en línea, y no depende de imágenes: la marca es texto y color.
    // Ojo: cada % literal se escribe %% porque el texto pasa por .formatted(); los tres %s son el enlace.
    private String buildEmailHtml(String enlace) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
              <title>Verifica tu correo electrónico</title>
              <link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@800&display=swap" rel="stylesheet">
            </head>
            <body bgcolor="#070B10" style="margin:0;padding:0;background-color:#070B10;">

              <!-- Texto de vista previa que se ve en la bandeja de entrada -->
              <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;font-size:1px;line-height:1px;color:#070B10;">
                Confirma tu correo y activa tu cuenta. El enlace dura 3 minutos.
              </div>

              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#070B10"
                    style="background-color:#070B10;">
                <tr><td align="center" style="padding:32px 12px;">

                  <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                        style="width:100%%;max-width:560px;">

                    <!-- MARCA: se arma con texto y color, sin imágenes -->
                    <tr><td style="padding:0 4px 18px;">
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td width="12" height="12" bgcolor="#D9A441"
                            style="width:12px;height:12px;border-radius:50%%;font-size:0;line-height:0;">&nbsp;</td>
                        <td style="padding-left:10px;font-family:'Bricolage Grotesque','Segoe UI',Helvetica,Arial,sans-serif;
                                  font-size:19px;font-weight:800;color:#FFFFFF;">
                          Reportes Urbanos
                        </td>
                      </tr></table>
                    </td></tr>

                    <!-- TARJETA -->
                    <tr><td bgcolor="#0E1620"
                            style="background-color:#0E1620;border:1px solid #26333F;border-radius:16px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">

                        <tr><td height="4" bgcolor="#D9A441"
                                style="height:4px;font-size:0;line-height:0;border-radius:16px 16px 0 0;">&nbsp;</td></tr>

                        <tr><td style="padding:36px 36px 8px;">
                          <h1 style="margin:0 0 16px;font-family:'Bricolage Grotesque','Segoe UI',Helvetica,Arial,sans-serif;
                                    font-size:30px;line-height:1.15;font-weight:800;color:#FFFFFF;letter-spacing:-0.5px;">
                            Confirma tu correo y activa tu cuenta
                          </h1>
                          <p style="margin:0 0 28px;font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                   font-size:16px;line-height:1.65;color:#C9D4D7;">
                            Gracias por unirte a <strong style="color:#FFFFFF;">Reportes Urbanos</strong>.
                            Con tu cuenta puedes reportar huecos en la vía, fugas de agua, alumbrado dañado
                            y más en tu barrio de Cartagena, y seguir cómo avanza cada reporte.
                          </p>

                          <!-- BOTÓN: color sólido, se ve aunque el correo bloquee las imágenes -->
                          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 28px;">
                            <tr><td bgcolor="#D9A441" align="center" style="border-radius:50px;background-color:#D9A441;">
                              <a href="%s"
                                style="display:inline-block;padding:15px 36px;font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                      font-size:16px;font-weight:bold;color:#070B10;text-decoration:none;border-radius:50px;">
                                Verificar correo electrónico
                              </a>
                            </td></tr>
                          </table>

                          <p style="margin:0 0 6px;font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                   font-size:13px;line-height:1.6;color:#A9B8BC;">
                            Si el botón no funciona, copia y pega este enlace en tu navegador:
                          </p>
                          <p style="margin:0 0 28px;font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                   font-size:13px;line-height:1.6;">
                            <a href="%s" style="color:#D9A441;word-break:break-all;">%s</a>
                          </p>
                        </td></tr>

                        <tr><td style="padding:0 36px 36px;">
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                            <tr><td style="border-top:1px solid #26333F;padding-top:20px;
                                          font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                          font-size:13px;line-height:1.6;color:#A9B8BC;">
                              <strong style="color:#FFFFFF;">Seguridad.</strong>
                              Este enlace expira en <strong style="color:#FFFFFF;">3 minutos</strong> y solo puede
                              usarse una vez. Si no creaste esta cuenta, ignora este correo.
                            </td></tr>
                          </table>
                        </td></tr>

                      </table>
                    </td></tr>

                    <!-- PIE -->
                    <tr><td align="center" style="padding:22px 8px 0;font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                                font-size:12px;line-height:1.6;color:#7F8E94;">
                      Reportes Urbanos &middot; Cartagena de Indias, Colombia<br>
                      Recibiste este correo porque alguien usó esta dirección para crear una cuenta.
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(enlace, enlace, enlace);
    }
}