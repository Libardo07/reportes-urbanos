package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.TipoReporte;
import com.reportes.urbanos.reportes_api.repository.*;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.ReporteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private ReporteService reporteService;  

    @Autowired
    private ReporteRepository reporteRepository;
    @Autowired
    private BarrioRepository barrioRepository;
    @Autowired
    private BarrioService barrioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Método utilitario para obtener el usuario logueado desde Spring Security
    private Usuario getUsuarioLogueado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email);
    }

    @ModelAttribute
    public void populateModelsWithCommonData(Model model) {
    model.addAttribute("barrios", barrioService.getBarriosOrdenados());
    model.addAttribute("tipos", TipoReporte.values());
}

    @GetMapping("/inicio")
    public String inicioCiudadano(Model model) {
        Usuario usuario = getUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("reporte", new Reporte());
        return "usuario_inicio";
    }

    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(Model model) {
        Usuario usuario = getUsuarioLogueado();
        model.addAttribute("reportes", reporteService.getReportesUsuario(usuario));
        return "usuario/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping(value = "/fragmento/formulario-reporte", produces = "text/html")
    public String fragmentoFormularioReporte(Model model) {
        model.addAttribute("reporte", new Reporte());
        return "usuario/fragments/formulario-reporte :: formulario-reporte";
    }

    @GetMapping(value = "/editar-reporte/{id}", produces = "text/html")
    public String fragmentoEditarReporte(@PathVariable String id, Model model) {
        Usuario usuario = getUsuarioLogueado();
        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null
                || reporte.getUsuario() == null
                || !reporte.getUsuario().getId().equals(usuario.getId())
                || !reporte.getEstado().equals(EstadoReporte.PENDIENTE)) {
            return "error :: error-content";
        }
        model.addAttribute("reporte", reporte);
        return "editar_reporte :: formulario-reporte";
    }

    @PostMapping("/guardar-reporte")
    public ResponseEntity<Map<String, String>> guardarReporte(@ModelAttribute Reporte reporte,
                                                            @RequestParam String barrioId,
                                                            @RequestParam String tipoReporte) {
        Map<String, String> response = new HashMap<>();
        Usuario usuario = getUsuarioLogueado();
        try {
            Barrio barrio = barrioRepository.findById(barrioId).orElse(null);
            TipoReporte tipo = TipoReporte.valueOf(tipoReporte);

            if (reporte.getId() != null) {
                Reporte reporteExistente = reporteRepository.findById(reporte.getId()).orElse(null);
                if (reporteExistente == null || !reporteExistente.getUsuario().getId().equals(usuario.getId())) {
                    response.put("error", "No se pudo editar: el reporte no existe o no pertenece a este usuario.");
                    return ResponseEntity.badRequest().body(response);
                }
                if (!reporteExistente.getEstado().equals(EstadoReporte.PENDIENTE)) {
                    response.put("error", "El reporte no puede editarse porque su estado es '" + reporteExistente.getEstado() + "'.");
                    return ResponseEntity.badRequest().body(response);
                }
                reporteExistente.setTitulo(reporte.getTitulo());
                reporteExistente.setDescripcion(reporte.getDescripcion());
                reporteExistente.setDireccion(reporte.getDireccion());
                reporteExistente.setBarrio(barrio);
                reporteExistente.setTipo(tipo);
                reporteExistente.preActualizar();
                reporteService.guardarReporte(reporteExistente);
                response.put("message", "Reporte actualizado correctamente.");
            } else {
                reporte.setBarrio(barrio);
                reporte.setTipo(tipo);
                reporte.setUsuario(usuario);
                reporte.preGuardar();
                reporteService.guardarReporte(reporte);
                response.put("message", "Reporte creado correctamente.");
            }
            response.put("success", "true");
        } catch (Exception e) {
            response.put("error", "Error al guardar el reporte: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/eliminar-reporte/{id}")
    public ResponseEntity<Map<String, String>> eliminarReporte(@PathVariable String id) {
        Map<String, String> response = new HashMap<>();
        Usuario usuario = getUsuarioLogueado();
        try {
            Reporte reporte = reporteRepository.findById(id).orElse(null);
            if (reporte == null || !reporte.getUsuario().getId().equals(usuario.getId())) {
                response.put("error", "Reporte no encontrado.");
                return ResponseEntity.badRequest().body(response);
            }
            if (!reporte.getEstado().equals(EstadoReporte.PENDIENTE)) {
                response.put("error", "Este reporte ya está en estado '" + reporte.getEstado() + "' y no puede ser eliminado.");
                return ResponseEntity.badRequest().body(response);
            }
            reporteService.eliminarReporte(reporte);
            response.put("success", "true");
            response.put("message", "Reporte eliminado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al eliminar el reporte: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}