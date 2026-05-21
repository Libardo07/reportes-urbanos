package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Repositorio para acceder a los tipos de reporte en la base de datos
public interface TipoReporteRepository extends JpaRepository<TipoReporte, Long> {
    Optional<TipoReporte> findByNombre(String nombre);
}