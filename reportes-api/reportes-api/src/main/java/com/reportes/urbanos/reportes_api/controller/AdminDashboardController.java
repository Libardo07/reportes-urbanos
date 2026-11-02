package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.DashboardService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Datos del dashboard propio del administrador (reemplaza al iframe de Power BI)
@RestController
@RequestMapping("/admin/api")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private EstadoReporteService estadoReporteService;

    @Autowired
    private TipoReporteService tipoReporteService;

    @Autowired
    private BarrioService barrioService;

    @GetMapping("/dashboard")
    public Map<String, Object> datos() {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("estados", estadoReporteService.getEstados().stream()
            .filter(e -> List.of("pendiente", "en_proceso", "resuelto").contains(e.getNombre().toLowerCase()))
            .map(e -> item(e.getId(), e.getNombre()))
            .collect(Collectors.toList()));
        respuesta.put("tipos", tipoReporteService.getTipos().stream()
            .map(t -> item(t.getId(), t.getNombre())).collect(Collectors.toList()));
        respuesta.put("barrios", barrioService.getBarriosOrdenados().stream()
            .map(b -> item(b.getId(), b.getNombre())).collect(Collectors.toList()));
        respuesta.put("reportes", dashboardService.getReportes());

        return respuesta;
    }

    private Map<String, Object> item(Long id, String nombre) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nombre", nombre);
        return m;
    }
}