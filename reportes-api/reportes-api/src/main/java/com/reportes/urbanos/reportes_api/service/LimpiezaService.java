package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.entity.VerificacionToken;
import com.reportes.urbanos.reportes_api.repository.PasswordResetTokenRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.repository.VerificacionTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class LimpiezaService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired 
    private UsuarioRepository usuarioRepository;

    @Autowired 
    private VerificacionTokenRepository tokenRepository;

    @Autowired 
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Cada 30 segundos: elimina usuarios no verificados con más de 90 segundos.
     * Esto da el margen justo después del cooldown de 60 s del registro.
     */
    @Scheduled(fixedDelay = 30_000)
    public void limpiarUsuariosPendientes() {
        LocalDateTime limite = LocalDateTime.now(BOGOTA).minusSeconds(90);
        List<Usuario> pendientes =
            usuarioRepository.findByVerificadoFalseAndFechaCreacionBefore(limite);

        for (Usuario u : pendientes) {
            VerificacionToken token = tokenRepository.findByEmail(u.getEmail());
            boolean tieneTokenActivo = token != null && !token.isExpirado() && !token.isUsado();
            if (!tieneTokenActivo) {
                tokenRepository.deleteByEmail(u.getEmail());
                usuarioRepository.delete(u);
            }
        }
    }

    /**
     * Cada 5 minutos: elimina tokens expirados y tokens ya utilizados.
     */
    @Scheduled(fixedDelay = 300_000)
    public void limpiarTokens() {
        LocalDateTime ahora = LocalDateTime.now(BOGOTA);

        List<VerificacionToken> expirados =
            tokenRepository.findByUsadoFalseAndExpiracionBefore(ahora);
        if (!expirados.isEmpty()) tokenRepository.deleteAll(expirados);

        List<VerificacionToken> usados = tokenRepository.findByUsadoTrue();
        if (!usados.isEmpty()) tokenRepository.deleteAll(usados);
    }



    // Cada 5 minutos: elimina tokens de contraseña expirados
    @Scheduled(fixedDelay = 300_000)
    public void limpiarTokensPassword() {
        List<PasswordResetToken> expirados =
            passwordResetTokenRepository.findByExpiracionBefore(
                LocalDateTime.now(BOGOTA)
            );
        if (!expirados.isEmpty())
            passwordResetTokenRepository.deleteAll(expirados);
    }
}