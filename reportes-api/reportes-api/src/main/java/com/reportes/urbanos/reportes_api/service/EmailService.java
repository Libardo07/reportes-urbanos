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

    // Envía un correo automático cuando un reporte cambia al estado "En Proceso"
    @Async
    public void enviarCorreoEnProceso(String emailUsuario, String nombreUsuario,
                                      String tituloReporte, String nombreAdmin) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("serviciodereporte@gmail.com", "Reportes Urbanos");
            helper.setTo(emailUsuario);
            helper.setSubject("Tu reporte está siendo atendido — Reportes Urbanos");

            // El diseño vive en CorreoPlantilla; aquí solo se envía (versión de texto y versión HTML)
            helper.setText(
                CorreoPlantilla.enProcesoTexto(appUrl, nombreUsuario, tituloReporte, nombreAdmin),
                CorreoPlantilla.enProcesoHtml(appUrl, nombreUsuario, tituloReporte, nombreAdmin));
            mailSender.send(mensaje);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR CORREO EN PROCESO: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    // Envía un correo al usuario cuando un reporte es marcado como resuelto
    @Async
    public void enviarCorreoResuelto(String emailUsuario, String nombreUsuario,
                                     String tituloReporte, String reporteId) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("serviciodereporte@gmail.com", "Reportes Urbanos");
            helper.setTo(emailUsuario);
            helper.setSubject("Tu reporte fue marcado como resuelto — Reportes Urbanos");

            String urlSi = appUrl + "/confirmacion/si/" + reporteId;
            String urlNo = appUrl + "/confirmacion/no/" + reporteId;

            helper.setText(
                CorreoPlantilla.resueltoTexto(nombreUsuario, tituloReporte, urlSi, urlNo),
                CorreoPlantilla.resueltoHtml(appUrl, nombreUsuario, tituloReporte, urlSi, urlNo));
            mailSender.send(mensaje);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR CORREO RESUELTO: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

}