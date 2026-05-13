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

    @DBRef(lazy = false)
    private Usuario usuario;

    private String reporteId;
    private String parentId;

    public Comentario(String texto, Usuario usuario, String reporteId) {
        this.texto = texto;
        this.usuario = usuario;
        this.reporteId = reporteId;
        this.fechaCreacion = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}