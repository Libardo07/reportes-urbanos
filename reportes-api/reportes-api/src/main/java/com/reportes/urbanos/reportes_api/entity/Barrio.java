package com.reportes.urbanos.reportes_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "barrio", schema = "reportes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barrio {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombre;


}
