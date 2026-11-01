package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.reportes.urbanos.reportes_api.dto.EstadisticaInicio;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int PAGE_SIZE = 10;

    // Obtiene todos los reportes para el administrador y los guarda en caché
    @Cacheable(value = "reportes-admin")
    public List<Reporte> getReportesAdmin() {
        return reporteRepository.findAllByOrderByFechaModificacionDesc();
    }

    // Obtiene los reportes de un usuario y los almacena en caché
    @Cacheable(value = "reportes-usuario", key = "#usuario.id")
    public List<Reporte> getReportesUsuario(Usuario usuario) {
        return reporteRepository.findByUsuarioOrderByFechaModificacionDesc(usuario);
    }

    // Cuenta los reportes del usuario por estado y tipo directamente en Mongo
    @Cacheable(value = "estadisticas-usuario", key = "#usuario.id")
    public List<EstadisticaInicio> getEstadisticasInicio(Usuario usuario) {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("usuario.$id").is(new ObjectId(usuario.getId()))),
            Aggregation.group("estadoReporteId", "tipoReporteId")
                .count().as("total")
                .max("fechaModificacion").as("ultima")
        );

        List<EstadisticaInicio> resultado = new ArrayList<>();
        for (Document d : mongoTemplate.aggregate(agg, "reportes", Document.class).getMappedResults()) {
            Document id = d.get("_id", Document.class);
            Number estado = id == null ? null : (Number) id.get("estadoReporteId");
            Number tipo = id == null ? null : (Number) id.get("tipoReporteId");
            Date ultima = d.getDate("ultima");

            resultado.add(new EstadisticaInicio(
                estado == null ? null : estado.longValue(),
                tipo == null ? null : tipo.longValue(),
                ((Number) d.get("total")).longValue(),
                ultima == null ? null : LocalDateTime.ofInstant(ultima.toInstant(), ZoneId.systemDefault())
            ));
        }
        return resultado;
    }

    // Obtiene los reportes del administrador de forma paginada
    public Page<Reporte> getReportesAdminPaginado(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return reporteRepository.findAllByOrderByFechaModificacionDesc(pageable);
    }

    // Obtiene los reportes del usuario y los divide en páginas para facilitar la navegación
    public Page<Reporte> getReportesUsuarioPaginado(Usuario usuario, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return reporteRepository.findByUsuarioOrderByFechaModificacionDesc(usuario, pageable);
    }

    // Busca reportes por título o muestra todos los reportes de forma paginada
    public Page<Reporte> explorarReportesPaginado(String q, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        if (q != null && !q.isBlank()) {
            return reporteRepository.findByTituloContainingIgnoreCaseOrderByFechaModificacionDesc(q, pageable);
        }
        return reporteRepository.findAllByOrderByFechaModificacionDesc(pageable);
    }

    // Guarda un reporte y limpia la caché de reportes actualizados
    @CacheEvict(value = {"reportes-admin", "reportes-usuario", "estadisticas-usuario"}, allEntries = true)
    public Reporte guardarReporte(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    // Elimina un reporte y actualiza la caché de reportes
    @CacheEvict(value = {"reportes-admin", "reportes-usuario", "estadisticas-usuario"}, allEntries = true)
    public void eliminarReporte(Reporte reporte) {
        reporteRepository.delete(reporte);
    }
}