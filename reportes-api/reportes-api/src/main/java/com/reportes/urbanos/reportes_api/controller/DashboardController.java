package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private ReporteService reporteService;
    @Autowired private EstadoReporteService estadoReporteService;
    @Autowired private TipoReporteService tipoReporteService;
    @Autowired private BarrioService barrioService;

    @GetMapping("/reportes")
    public List<Map<String, Object>> getReportes() {

        Map<Long, String> estados = estadoReporteService.getEstados().stream()
            .collect(Collectors.toMap(EstadoReporte::getId, EstadoReporte::getNombre));
        Map<Long, String> tipos = tipoReporteService.getTipos().stream()
            .collect(Collectors.toMap(TipoReporte::getId, TipoReporte::getNombre));
        Map<Long, String> barrios = barrioService.getBarriosOrdenados().stream()
            .collect(Collectors.toMap(Barrio::getId, Barrio::getNombre));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return reporteService.getReportesAdmin().stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();

            String estado = estados.getOrDefault(r.getEstadoReporteId(), "Desconocido");
            String tipo   = tipos.getOrDefault(r.getTipoReporteId(), "Desconocido");
            String barrio = barrios.getOrDefault(r.getBarrioId(), "Desconocido");

            long diasResolucion = 0;
            if (r.getFechaCreacion() != null && r.getFechaModificacion() != null) {
                diasResolucion = java.time.Duration.between(
                    r.getFechaCreacion(), r.getFechaModificacion()).toDays();
            }

            String rangoResolucion;
            if (diasResolucion <= 7)       rangoResolucion = "0-7 días";
            else if (diasResolucion <= 30)  rangoResolucion = "8-30 días";
            else if (diasResolucion <= 90)  rangoResolucion = "31-90 días";
            else                            rangoResolucion = "+90 días";

            row.put("id_reporte",        r.getId());
            row.put("titulo",            r.getTitulo());
            row.put("categoria",         tipo);
            row.put("estado",            estado);
            row.put("es_resuelto",       "Resuelto".equals(estado) ? 1 : 0);
            row.put("barrio",            barrio);
            row.put("usuario",           r.getUsuario() != null ? r.getUsuario().getNombre() : "Desconocido");
            row.put("fecha_reporte",     r.getFechaCreacion() != null ? r.getFechaCreacion().format(fmt) : null);
            row.put("fecha_actualizacion", r.getFechaModificacion() != null ? r.getFechaModificacion().format(fmt) : null);
            row.put("anio_reporte",      r.getFechaCreacion() != null ? r.getFechaCreacion().getYear() : null);
            row.put("mes_reporte",       r.getFechaCreacion() != null ? r.getFechaCreacion().getMonthValue() : null);
            row.put("nombre_mes",        r.getFechaCreacion() != null ? r.getFechaCreacion().getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, new Locale("es")) : null);
            row.put("trimestre",         r.getFechaCreacion() != null ? "T" + ((r.getFechaCreacion().getMonthValue()-1)/3+1) : null);
            row.put("dias_resolucion",   diasResolucion);
            row.put("rango_resolucion",  rangoResolucion);

            return row;
        }).collect(Collectors.toList());
    }

    @GetMapping("/barrios")
    public List<Map<String, Object>> getBarrios() {
        return barrioService.getBarriosOrdenados().stream().map(b -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id_barrio", b.getId());
            row.put("nombre", b.getNombre());
            return row;
        }).collect(Collectors.toList());
    }

    @GetMapping("/estados")
    public List<Map<String, Object>> getEstados() {
        return estadoReporteService.getEstados().stream().map(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id_estado", e.getId());
            row.put("nombre", e.getNombre());
            return row;
        }).collect(Collectors.toList());
    }

    @GetMapping("/tipos")
    public List<Map<String, Object>> getTipos() {
        return tipoReporteService.getTipos().stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id_tipo", t.getId());
            row.put("nombre", t.getNombre());
            return row;
        }).collect(Collectors.toList());
    }

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/usuarios")
    public List<Map<String, Object>> getUsuarios() {
        return usuarioRepository.findAll().stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id_usuario", u.getId());
            row.put("nombre", u.getNombre());
            row.put("email", u.getEmail());
            row.put("rol", u.getRol());
            row.put("fecha_registro", u.getFechaCreacion() != null ? 
                u.getFechaCreacion().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
            return row;
        }).collect(Collectors.toList());
    }
}