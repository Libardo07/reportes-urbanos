package com.reportes.urbanos.reportes_api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
// Documento de MongoDB que almacena los comentarios 
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

    // Comentario principal del hilo (null si este comentario es uno principal)
    private String parentId;

    // Comentario exacto al que se responde (puede ser el principal o otra respuesta)
    // y el nombre de su autor. Son null en los comentarios principales y en las respuestas antiguas.
    private String respondeAId;

    private String respondeANombre;

    public Comentario(String texto, Usuario usuario, String reporteId) {
        this.texto = texto;
        this.usuarioId = usuario.getId();
        this.usuarioNombre = usuario.getNombre();
        this.reporteId = reporteId;
        this.fechaCreacion = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}