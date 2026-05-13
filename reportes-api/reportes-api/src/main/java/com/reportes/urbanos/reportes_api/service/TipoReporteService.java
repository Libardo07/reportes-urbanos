package com.reportes.urbanos.reportes_api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reportes.urbanos.reportes_api.entity.TipoReporte;
import com.reportes.urbanos.reportes_api.repository.TipoReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Service
public class TipoReporteService {

    @Autowired
    private TipoReporteRepository tipoReporteRepository;

    @Cacheable(value = "tipos-reporte")
    public List<TipoReporte> getTipos() {
        return tipoReporteRepository.findAll();
    }
}
