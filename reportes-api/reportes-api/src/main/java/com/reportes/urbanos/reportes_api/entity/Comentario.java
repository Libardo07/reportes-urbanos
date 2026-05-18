package com.reportes.urbanos.reportes_api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Document(collection = "comentarios")
@Getter @Setter @NoArgsConstructor
public class Comentario {

    @Id
    private String id;

    private String texto;

    private LocalDateTime fechaCreacion;

    private String usuarioId;
    private String usuarioNombre;

    private String reporteId;
    private String parentId;

    public Comentario(String texto, Usuario usuario, String reporteId) {
        this.texto = texto;
        this.usuarioId = usuario.getId();
        this.usuarioNombre = usuario.getNombre();
        this.reporteId = reporteId;
        this.fechaCreacion = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}