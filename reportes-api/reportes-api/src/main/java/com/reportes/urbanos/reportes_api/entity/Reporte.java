package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

import com.reportes.urbanos.reportes_api.enums.EstadoReporte;

@Entity // entidada en la base de datos
@Data // genera getters, setters, toString, equals, hashCode
@NoArgsConstructor // genera constructor sin parametros
@AllArgsConstructor // con parametros
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // generacion automaticamente 
    private Long id;


    @Column(nullable = false) // los campos no pueden estar vacios en la base de datos
    @Size(min = 5, max = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition= "TEXT") // textos largos 
    private String descripcion;

    @Column(nullable = false , length = 150)
    private String direccion;


    @Enumerated(EnumType.STRING)
    private EstadoReporte estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne // relacion
    @JoinColumn(name = "usuario_id" , nullable = false) // clave foranea 
    private Usuario usuario;

    @ManyToOne
    @JoinColumn (name = "usuario_admin_id")
    private Usuario usuarioAdmin;

    @ManyToOne
    @JoinColumn (name = "tipo_id", nullable = false)
    private Tipo tipo;

    @ManyToOne
    @JoinColumn (name = "barrio_id", nullable = false)
    private Barrio barrio;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;


    @PrePersist // se ejecuta antes de guardar el objeto
    protected void preGuardar() {
        fechaCreacion = LocalDateTime.now();
        fechaModificacion = fechaCreacion; 
        if (estado == null) {
            estado = EstadoReporte.PENDIENTE;
        }
    }

    @PreUpdate // se ejecuta la modificar el reporte 
    protected void preActualizar(){
        fechaModificacion = LocalDateTime.now();
    }

}
