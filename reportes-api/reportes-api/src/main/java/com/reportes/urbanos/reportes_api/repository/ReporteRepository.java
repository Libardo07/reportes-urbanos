package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReporteRepository extends MongoRepository<Reporte, String> {
    List<Reporte> findByUsuarioOrderByFechaModificacionDesc(Usuario usuario);
    List<Reporte> findAllByOrderByFechaModificacionDesc();
    List<Reporte> findAllByOrderByFechaCreacionDesc();
    Page<Reporte> findAllByOrderByFechaModificacionDesc(Pageable pageable);
    Page<Reporte> findByUsuarioOrderByFechaModificacionDesc(Usuario usuario, Pageable pageable);
    Page<Reporte> findByTituloContainingIgnoreCaseOrderByFechaModificacionDesc(String titulo, Pageable pageable);  
        // Contar por estadoReporteId
    long countByEstadoReporteId(Long estadoReporteId);

    // Contar por tipoReporteId
    long countByTipoReporteId(Long tipoReporteId);

    // Contar por barrioId
    long countByBarrioId(Long barrioId);
}

