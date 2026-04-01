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

    private LocalDateTime expiracion;

    public PasswordResetToken(String token, String email) {
        this.token = token;
        this.email = email;
        // El token expira en 30 minutos
        this.expiracion = LocalDateTime.now().plusMinutes(30);
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiracion);
    }
}
