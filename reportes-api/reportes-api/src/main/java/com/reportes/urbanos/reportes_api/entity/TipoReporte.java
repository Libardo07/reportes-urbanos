package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_reporte")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;
}