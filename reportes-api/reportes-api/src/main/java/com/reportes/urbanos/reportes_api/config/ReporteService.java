package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Cacheable(value = "reportes-admin")
    public List<Reporte> getReportesAdmin() {
        return reporteRepository.findAllByOrderByFechaModificacionDesc();
    }

    @Cacheable(value = "reportes-usuario", key = "#usuario.id")
    public List<Reporte> getReportesUsuario(Usuario usuario) {
        return reporteRepository.findByUsuarioOrderByFechaModificacionDesc(usuario);
    }

    @CacheEvict(value = {"reportes-admin", "reportes-usuario"}, allEntries = true)
    public Reporte guardarReporte(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    @CacheEvict(value = {"reportes-admin", "reportes-usuario"}, allEntries = true)
    public void eliminarReporte(Reporte reporte) {
        reporteRepository.delete(reporte);
    }
}