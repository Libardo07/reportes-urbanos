package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.VerificacionCodigo;
import com.reportes.urbanos.reportes_api.repository.VerificacionCodigoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VerificacionService {

    @Autowired
    private VerificacionCodigoRepository verificacionRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCodigo(String email) {
        verificacionRepository.deleteByEmail(email);
        String codigo = String.format("%06d", new Random().nextInt(999999));
        VerificacionCodigo verificacion = new VerificacionCodigo(email, codigo);
        verificacionRepository.save(verificacion);

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Código de verificación - Reportes Urbanos");
        mensaje.setText("Tu código de verificación es: " + codigo + "\n\nEste código expira en 5 minutos.\n\nReportes Urbanos - Cartagena de Indias");
        mailSender.send(mensaje);
    }

    public String verificarCodigo(String email, String codigo) {
        VerificacionCodigo verificacion = verificacionRepository.findByEmail(email);

        if (verificacion == null) {
            return "EXPIRADO";
        }
        if (verificacion.isExpirado()) {
            verificacionRepository.deleteByEmail(email);
            return "EXPIRADO";
        }
        if (verificacion.getIntentos() >= 5) {
            return "BLOQUEADO";
        }
        if (!verificacion.getCodigo().equals(codigo)) {
            verificacion.setIntentos(verificacion.getIntentos() + 1);
            verificacionRepository.save(verificacion);
            return "INCORRECTO:" + (5 - verificacion.getIntentos());
        }

        verificacionRepository.deleteByEmail(email);
        return "OK";
    }

    public String reenviarCodigo(String email) {
        VerificacionCodigo verificacion = verificacionRepository.findByEmail(email);
        int reenvios = verificacion != null ? verificacion.getReenvios() : 0;

        enviarCodigo(email);

        VerificacionCodigo nueva = verificacionRepository.findByEmail(email);
        nueva.setReenvios(reenvios + 1);
        verificacionRepository.save(nueva);

        return "REENVIADO:" + (reenvios + 1);
    }
}
