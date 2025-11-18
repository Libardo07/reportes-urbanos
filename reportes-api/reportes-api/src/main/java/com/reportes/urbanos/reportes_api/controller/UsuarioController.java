package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {
    @Autowired
    private ReporteRepository reporteRepository;
    @Autowired
    private BarrioRepository barrioRepository;
    @Autowired
    private TipoRepository tipoRepository;

    @GetMapping("/inicio")
    public String inicioCiudadano(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("reporte", new Reporte());
        model.addAttribute("barrios", barrioRepository.findAll());
        model.addAttribute("tipos", tipoRepository.findAll());
        
        return "usuario_inicio";
    }

    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("reportes", reporteRepository.findByUsuario(usuario));
        return "usuario/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping(value = "/fragmento/formulario-reporte", produces = "text/html")
    public String fragmentoFormularioReporte(Model model) {
        model.addAttribute("reporte", new Reporte());
        model.addAttribute("barrios", barrioRepository.findAll());
        model.addAttribute("tipos", tipoRepository.findAll());
        return "usuario/fragments/formulario-reporte :: formulario-reporte";
    }
    
    @GetMapping(value = "/editar-reporte/{id}", produces = "text/html")
    public String fragmentoEditarReporte(@PathVariable Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        Reporte reporte = reporteRepository.findById(id).orElse(null);
        
        if (reporte == null || !reporte.getUsuario().getId().equals(usuario.getId()) || !reporte.getEstado().equals(EstadoReporte.PENDIENTE)) {
            return "error :: error-content"; 
        }
        
        model.addAttribute("reporte", reporte);
        model.addAttribute("barrios", barrioRepository.findAll());
        model.addAttribute("tipos", tipoRepository.findAll());
        return "editar_reporte :: formulario-reporte"; 
    }


    @PostMapping("/guardar-reporte")
    public ResponseEntity<Map<String, String>> guardarReporte(@ModelAttribute Reporte reporte,
                                                            @RequestParam Long barrioId,
                                                            @RequestParam Long tipoId,
                                                            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
            response.put("error", "Usuario no autorizado.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            Barrio barrio = barrioRepository.findById(barrioId).orElse(null);
            Tipo tipo = tipoRepository.findById(tipoId).orElse(null);

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
                reporteRepository.save(reporteExistente);
                response.put("message", "Reporte actualizado correctamente.");
            } else { // Es una creación
                reporte.setBarrio(barrio);
                reporte.setTipo(tipo);
                reporte.setUsuario(usuario);
                reporteRepository.save(reporte);
                response.put("message", "Reporte creado correctamente.");
            }
            response.put("success", "true");
        } catch (Exception e) {
            response.put("error", "Error al guardar el reporte: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/eliminar-reporte/{id}")
    public ResponseEntity<Map<String, String>> eliminarReporte(@PathVariable Long id, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
            response.put("error", "Usuario no autorizado.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
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
            reporteRepository.delete(reporte);
            response.put("success", "true");
            response.put("message", "Reporte eliminado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al eliminar el reporte: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}