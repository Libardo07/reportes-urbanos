package com.reportes.urbanos.reportes_api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "password_reset_tokens")
@Getter @Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    private String id;

    private String token;

    private String email;

    private String tipo;

    private LocalDateTime expiracion;

    public PasswordResetToken(String token, String email) {
        this.token = token;
        this.email = email;
        this.tipo = "PASSWORD_RESET";
        // El token expira en 30 minutos
        this.expiracion = LocalDateTime.now().plusMinutes(30);
    }


    // ← NUEVO constructor para verificación de email
    public PasswordResetToken(String token, String email, String tipo) {
        this.token = token;
        this.email = email;
        this.tipo = tipo;
        this.expiracion = tipo.equals("EMAIL_VERIFICATION")
            ? LocalDateTime.now().plusHours(24)  // 24 horas para verificación
            : LocalDateTime.now().plusMinutes(30); // 30 min para reset
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiracion);
    }
}
