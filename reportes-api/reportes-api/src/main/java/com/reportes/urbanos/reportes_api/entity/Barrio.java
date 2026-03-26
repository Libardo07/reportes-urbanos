package com.reportes.urbanos.reportes_api.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Document(collection = "barrio")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barrio {
    @Id
    private String id;
    private String nombre;
}
