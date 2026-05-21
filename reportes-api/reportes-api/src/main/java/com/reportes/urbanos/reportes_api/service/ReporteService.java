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

import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

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
    @CacheEvict(value = {"reportes-admin", "reportes-usuario"}, allEntries = true)
    public Reporte guardarReporte(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    // Elimina un reporte y actualiza la caché de reportes
    @CacheEvict(value = {"reportes-admin", "reportes-usuario"}, allEntries = true)
    public void eliminarReporte(Reporte reporte) {
        reporteRepository.delete(reporte);
    }
}