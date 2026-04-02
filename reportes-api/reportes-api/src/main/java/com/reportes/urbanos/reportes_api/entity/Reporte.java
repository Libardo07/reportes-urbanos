package com.reportes.urbanos.reportes_api.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.TipoReporte;


import org.springframework.data.mongodb.core.index.CompoundIndexes;


@CompoundIndexes({
    @CompoundIndex(name = "idx_usuario_fecha", def = "{'usuario.$id': 1, 'fechaModificacion': -1}"),
    @CompoundIndex(name = "idx_fecha_modificacion", def = "{'fechaModificacion': -1}")
})
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
