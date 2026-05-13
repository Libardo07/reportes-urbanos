package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogoService {

    @Autowired private EstadoReporteRepository estadoRepo;
    @Autowired private TipoReporteRepository   tipoRepo;
    @Autowired private BarrioRepository        barrioRepo;

    @Cacheable("estadosMap")
    public Map<Long, String> getEstadosMap() {
        return estadoRepo.findAll().stream()
            .collect(Collectors.toMap(EstadoReporte::getId, EstadoReporte::getNombre));
    }

    @Cacheable("tiposMap")
    public Map<Long, String> getTiposMap() {
        return tipoRepo.findAll().stream()
            .collect(Collectors.toMap(TipoReporte::getId, TipoReporte::getNombre));
    }

    @Cacheable("barriosMap")
    public Map<Long, String> getBarriosMap() {
        return barrioRepo.findAll().stream()
            .collect(Collectors.toMap(Barrio::getId, Barrio::getNombre));
    }
}