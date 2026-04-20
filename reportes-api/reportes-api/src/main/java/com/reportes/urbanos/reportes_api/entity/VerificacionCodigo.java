package com.reportes.urbanos.reportes_api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Document(collection = "verificacion_codigos")
@Getter @Setter
@NoArgsConstructor
public class VerificacionCodigo {

    @Id
    private String id;
    private String email;
    private String codigo;
    private LocalDateTime expiracion;
    private int intentos = 0;
    private int reenvios = 0;

    public VerificacionCodigo(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
        this.expiracion = LocalDateTime.now(ZoneId.of("America/Bogota")).plusMinutes(5);
    }

    public boolean isExpirado() {
        return LocalDateTime.now(ZoneId.of("America/Bogota")).isAfter(expiracion);
    }
}
