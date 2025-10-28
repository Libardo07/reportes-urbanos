package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findByUsuario(Usuario usuario);
}

