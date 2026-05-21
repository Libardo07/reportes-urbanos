package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Repositorio para acceder a la tabla de barrios en la base de datos
public interface BarrioRepository extends JpaRepository<Barrio, Long> {
    Optional<Barrio> findByNombre(String nombre);
}