package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.dto.ReporteLigero;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Datos livianos para el dashboard del administrador (caché de 30 s, casi en vivo)
@Service
public class DashboardService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MongoTemplate mongoTemplate;

    @Cacheable(value = "dashboard", key = "'reportes'")
    public List<ReporteLigero> getReportes() {
        // Solo se piden 4 campos y se lee como Document: así no se resuelven los @DBRef
        Query query = new Query();
        query.fields()
            .include("estadoReporteId")
            .include("tipoReporteId")
            .include("barrioId")
            .include("fechaCreacion");

        List<ReporteLigero> filas = new ArrayList<>();
        for (Document d : mongoTemplate.find(query, Document.class, "reportes")) {
            Date fecha = d.getDate("fechaCreacion");
            if (fecha == null) continue;
            long creado = LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault())
                .atZone(BOGOTA).toEpochSecond();
            filas.add(new ReporteLigero(
                num(d.get("estadoReporteId")),
                num(d.get("tipoReporteId")),
                num(d.get("barrioId")),
                creado
            ));
        }
        return filas;
    }

    private long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}