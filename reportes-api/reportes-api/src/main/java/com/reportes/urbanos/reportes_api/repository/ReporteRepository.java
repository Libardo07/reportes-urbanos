package com.reportes.urbanos.reportes_api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.reportes.urbanos.reportes_api.model.Reporte;
import com.reportes.urbanos.reportes_api.model.Usuario;

import java.util.List;

@Repository
public interface ReporteRepository extends MongoRepository<Reporte, String> 
{
    List<Reporte> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);
    List<Reporte> findAllByOrderByFechaCreacionDesc();
}

