package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Comentario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

// Repositorio de MongoDB para gestionar comentarios de los reportes
public interface ComentarioRepository extends MongoRepository<Comentario, String> {
    List<Comentario> findByReporteIdAndParentIdIsNullOrderByFechaCreacionDesc(String reporteId);
    List<Comentario> findByParentIdOrderByFechaCreacionAsc(String parentId);
    List<Comentario> findByReporteIdOrderByFechaCreacionDesc(String reporteId);
    void deleteByReporteId(String reporteId);
    long countByReporteId(String reporteId);
    List<Comentario> findByParentId(String parentId);
}
