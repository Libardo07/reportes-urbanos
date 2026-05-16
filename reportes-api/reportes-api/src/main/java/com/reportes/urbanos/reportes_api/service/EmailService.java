package com.reportes.urbanos.reportes_api.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.base-url}")
    private String appUrl;

    // ── Correo: Estado cambiado a En Proceso ─────────────────────────────────
    @Async
    public void enviarCorreoEnProceso(String emailUsuario, String nombreUsuario,
                                      String tituloReporte, String nombreAdmin) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(emailUsuario);
            helper.setSubject("Tu reporte está siendo atendido 🔧");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f0f4ff;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:560px;margin:40px auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(21,88,176,0.1);">
                    
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#1558b0,#1a73e8);padding:32px 32px 24px;text-align:center;">
                      <div style="font-size:40px;margin-bottom:8px;">🔧</div>
                      <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;">Tu reporte está en proceso</h1>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px;">
                      <p style="color:#1a237e;font-size:16px;margin:0 0 16px;">Hola <strong>%s</strong>,</p>
                      <p style="color:#3c4043;font-size:15px;line-height:1.6;margin:0 0 24px;">
                        Tu reporte ha sido recibido y un administrador ya está trabajando en él.
                      </p>

                      <!-- Reporte card -->
                      <div style="background:#f0f4ff;border-left:4px solid #1a73e8;border-radius:8px;padding:16px 20px;margin-bottom:24px;">
                        <p style="margin:0 0 6px;font-size:12px;color:#5c6bc0;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Reporte</p>
                        <p style="margin:0;font-size:16px;font-weight:600;color:#1a237e;">%s</p>
                      </div>

                      <!-- Info -->
                      <div style="display:flex;gap:12px;margin-bottom:28px;">
                        <div style="flex:1;background:#f8f9ff;border-radius:8px;padding:14px 16px;border:1px solid #e8eeff;">
                          <p style="margin:0 0 4px;font-size:11px;color:#5c6bc0;font-weight:700;text-transform:uppercase;">Admin asignado</p>
                          <p style="margin:0;font-size:14px;font-weight:600;color:#1a237e;">%s</p>
                        </div>
                      </div>

                      <p style="color:#3c4043;font-size:14px;line-height:1.6;margin:0 0 28px;">
                        Te notificaremos cuando el problema sea resuelto. Puedes ver el estado de tu reporte en cualquier momento desde la plataforma.
                      </p>

                      <!-- Botón -->
                      <div style="text-align:center;">
                        <a href="%s/usuario/inicio" 
                            style="display:inline-block;background:linear-gradient(135deg,#1558b0,#1a73e8);color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;">
                          Ver mis reportes
                        </a>
                      </div>
                    </div>

                    <!-- Footer -->
                    <div style="background:#f8f9ff;padding:20px 32px;text-align:center;border-top:1px solid #e8eeff;">
                      <p style="margin:0;font-size:12px;color:#9e9e9e;">Sistema de Reportes Urbanos — Cartagena de Indias</p>
                    </div>

                  </div>
                </body>
                </html>
                """.formatted(nombreUsuario, tituloReporte, nombreAdmin, appUrl);

            helper.setText(html, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo En Proceso: " + e.getMessage());
        }
    }

    @Async
    public void enviarCorreoResuelto(String emailUsuario, String nombreUsuario,
                                    String tituloReporte, String reporteId) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(emailUsuario);
            helper.setSubject("Tu reporte fue marcado como resuelto ✅");

            String urlSi = appUrl + "/confirmacion/si/" + reporteId;
            String urlNo = appUrl + "/confirmacion/no/" + reporteId;

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f0f4ff;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:560px;margin:40px auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(21,88,176,0.1);">

                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#1a8a4a,#2ecc71);padding:32px 32px 24px;text-align:center;">
                      <div style="font-size:40px;margin-bottom:8px;">✅</div>
                      <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;">Reporte marcado como resuelto</h1>
                    </div>

                    <!-- Body -->
                    <div style="padding:32px;">
                      <p style="color:#1a237e;font-size:16px;margin:0 0 16px;">Hola <strong>%s</strong>,</p>
                      <p style="color:#3c4043;font-size:15px;line-height:1.6;margin:0 0 24px;">
                        El equipo de gestión urbana ha marcado tu reporte como resuelto. 
                        Queremos saber si el problema fue solucionado correctamente.
                      </p>

                      <!-- Reporte card -->
                      <div style="background:#eaf3de;border-left:4px solid #2ecc71;border-radius:8px;padding:16px 20px;margin-bottom:28px;">
                        <p style="margin:0 0 6px;font-size:12px;color:#3b6d11;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Reporte resuelto</p>
                        <p style="margin:0;font-size:16px;font-weight:600;color:#1a237e;">%s</p>
                      </div>

                      <p style="color:#1a237e;font-size:15px;font-weight:600;text-align:center;margin:0 0 20px;">
                        ¿El problema fue resuelto satisfactoriamente?
                      </p>

                      <!-- Botones -->
                      <div style="text-align:center;margin-bottom:28px;">
                        <a href="%s"
                          style="display:inline-block;background:linear-gradient(135deg,#1a8a4a,#2ecc71);color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:8px;font-size:15px;font-weight:600;">
                          ✅ Sí, fue resuelto
                        </a>
                        &nbsp;&nbsp;&nbsp;&nbsp;
                        <a href="%s"
                          style="display:inline-block;background:linear-gradient(135deg,#c0392b,#e74c3c);color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:8px;font-size:15px;font-weight:600;">
                          ❌ No, persiste
                        </a>
                      </div>

                      <p style="color:#9e9e9e;font-size:12px;text-align:center;margin:0;">
                        Tu opinión nos ayuda a mejorar la gestión urbana de Cartagena.
                      </p>
                    </div>

                    <!-- Footer -->
                    <div style="background:#f8f9ff;padding:20px 32px;text-align:center;border-top:1px solid #e8eeff;">
                      <p style="margin:0;font-size:12px;color:#9e9e9e;">Sistema de Reportes Urbanos — Cartagena de Indias</p>
                    </div>

                  </div>
                </body>
                </html>
                """.formatted(nombreUsuario, tituloReporte, urlSi, urlNo);

            helper.setText(html, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo Resuelto: " + e.getMessage());
        }
    }
}