package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Repositorio para acceder a los estados de los reportes en la base de datos
public interface EstadoReporteRepository extends JpaRepository<EstadoReporte, Long> {
    Optional<EstadoReporte> findByNombre(String nombre);
}