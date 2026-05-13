package com.reportes.urbanos.reportes_api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import com.reportes.urbanos.reportes_api.repository.EstadoReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Service
public class EstadoReporteService {

    @Autowired
    private EstadoReporteRepository estadoReporteRepository;

    @Cacheable(value = "estados-reporte")
    public List<EstadoReporte> getEstados() {
        return estadoReporteRepository.findAll();
    }

    @Cacheable(value = "estados-reporte", key = "#nombre")
    public EstadoReporte getByNombre(String nombre) {
        return estadoReporteRepository.findByNombre(nombre).orElseThrow();
    }
}