package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReporteRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Reporte> filtrar(
            Long estadoReporteId,
            Long tipoReporteId,
            Long barrioId,
            String nombreUsuario,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {

        List<Criteria> criterias = new ArrayList<>();

        if (estadoReporteId != null)
            criterias.add(Criteria.where("estadoReporteId").is(estadoReporteId));

        if (tipoReporteId != null)
            criterias.add(Criteria.where("tipoReporteId").is(tipoReporteId));

        if (barrioId != null)
            criterias.add(Criteria.where("barrioId").is(barrioId));

        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            List<String> ids = mongoTemplate.findAll(
                com.reportes.urbanos.reportes_api.entity.Usuario.class)
                .stream()
                .filter(u -> u.getNombre().toLowerCase()
                    .contains(nombreUsuario.toLowerCase().trim()))
                .map(u -> u.getId())
                .toList();
            criterias.add(Criteria.where("usuario.$id").in(
                ids.stream().map(org.bson.types.ObjectId::new).toList()));
        }

        if (fechaDesde != null)
            criterias.add(Criteria.where("fechaModificacion").gte(fechaDesde));

        if (fechaHasta != null)
            criterias.add(Criteria.where("fechaModificacion").lte(fechaHasta));

        Query query = new Query();
        if (!criterias.isEmpty())
            query.addCriteria(new Criteria().andOperator(criterias.toArray(new Criteria[0])));

        query.with(org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC, "fechaModificacion"));

        return mongoTemplate.find(query, Reporte.class);
    }
}