package com.reportes.urbanos.reportes_api.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.TipoReporte;

@Document(collection = "reportes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    private String direccion;

    private EstadoReporte estado;

    private TipoReporte tipo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaModificacion;

    // Embebido
    private Barrio barrio;

    // Referencias
    @DBRef
    private Usuario usuario;

    @DBRef
    private Usuario usuarioAdmin;

    public void preGuardar() {
        fechaCreacion = LocalDateTime.now(ZoneId.of("America/Bogota"));
        fechaModificacion = fechaCreacion;
        if (estado == null) {
            estado = EstadoReporte.PENDIENTE;
        }
    }

    public void preActualizar() {
        fechaModificacion = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
