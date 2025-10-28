package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tipo", schema = "reportes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tipo {
    @Id
    private Long id;

    @Column(nullable = false)
    private String nombre;


}
