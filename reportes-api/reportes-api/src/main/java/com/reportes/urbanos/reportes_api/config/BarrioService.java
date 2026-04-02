package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import com.reportes.urbanos.reportes_api.repository.BarrioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BarrioService {

    @Autowired
    private BarrioRepository barrioRepository;

    @Cacheable(value = "barrios")
    public List<Barrio> getBarriosOrdenados() {
        return barrioRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Barrio::getNombre))
                .collect(Collectors.toList());
    }
}