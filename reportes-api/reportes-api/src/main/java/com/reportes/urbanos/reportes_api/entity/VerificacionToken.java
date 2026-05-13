package com.reportes.urbanos.reportes_api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Document(collection = "verificacion_tokens")
@Getter @Setter @NoArgsConstructor
public class VerificacionToken {

    @Id
    private String id;

    private String email;

    private String token;

    private LocalDateTime expiracion;

    private boolean usado = false;
    
    private int reenvios = 0;

    public VerificacionToken(String email, String token) {
        this.email     = email;
        this.token     = token;
        this.expiracion = LocalDateTime.now(ZoneId.of("America/Bogota")).plusMinutes(3);
    }

    public boolean isExpirado() {
        return LocalDateTime.now(ZoneId.of("America/Bogota")).isAfter(expiracion);
    }
}