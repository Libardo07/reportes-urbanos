package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.repository.*;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
import com.reportes.urbanos.reportes_api.service.S3Service;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.List;



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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private TipoReporteRepository tipoReporteRepository;

    @Autowired
    private EstadoReporteRepository estadoReporteRepository; 

    @Autowired
    private BarrioService barrioService;

    @Autowired
    private TipoReporteService tipoReporteService;

    @Autowired
    private EstadoReporteService estadoReporteService;
    

    // Método utilitario para obtener el usuario logueado desde Spring Security
    private Usuario getUsuarioLogueado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email);
    }

    @ModelAttribute
    public void populateModelsWithCommonData(Model model) {
        List<Barrio> barrios = barrioService.getBarriosOrdenados();
        List<TipoReporte> tipos = tipoReporteService.getTipos();
        List<EstadoReporte> estados = estadoReporteService.getEstados();

        model.addAttribute("barrios", barrios);
        model.addAttribute("tipos", tipos);
        model.addAttribute("estadosMap",
            estados.stream().collect(Collectors.toMap(EstadoReporte::getId, EstadoReporte::getNombre)));
        model.addAttribute("tiposMap",
            tipos.stream().collect(Collectors.toMap(TipoReporte::getId, TipoReporte::getNombre)));
        model.addAttribute("barriosMap",
            barrios.stream().collect(Collectors.toMap(Barrio::getId, Barrio::getNombre)));
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
        EstadoReporte pendiente = estadoReporteRepository.findByNombre("Pendiente").orElseThrow();
        if (reporte == null
                || reporte.getUsuario() == null
                || !reporte.getUsuario().getId().equals(usuario.getId())
                || !reporte.getEstadoReporteId().equals(pendiente.getId())) {
            return "error :: error-content";
        }
        model.addAttribute("reporte", reporte);
        return "editar_reporte :: formulario-reporte";
    }

    @PostMapping("/guardar-reporte")

    
    public ResponseEntity<Map<String, String>> guardarReporte(
            @RequestParam(required = false) String id,
            @RequestParam String titulo,
            @RequestParam String descripcion,
            @RequestParam String direccion,
            @RequestParam String barrioId,
            @RequestParam String tipoReporte,
            @RequestParam(value = "imagenes", required = false) List<MultipartFile> imagenes) {

                System.out.println("=== IMAGENES RECIBIDAS: " + (imagenes != null ? imagenes.size() : "null"));
                if (imagenes != null) {
                    imagenes.forEach(img -> System.out.println("  - " + img.getOriginalFilename() + " | " + img.getSize()));
                }

        Map<String, String> response = new HashMap<>();
        Usuario usuario = getUsuarioLogueado();

        try {

            Barrio barrio = barrioRepository.findById(Long.parseLong(barrioId)).orElse(null);
            TipoReporte tipo = tipoReporteRepository.findByNombre(tipoReporte).orElseThrow();
            EstadoReporte pendiente = estadoReporteRepository.findByNombre("Pendiente").orElseThrow();

            if (id != null && !id.isBlank()) {
                Reporte reporteExistente = reporteRepository.findById(id).orElse(null);
                if (reporteExistente == null ||
                    !reporteExistente.getUsuario().getId().equals(usuario.getId())) {
                    response.put("error", "No se pudo editar: el reporte no existe o no pertenece a este usuario.");
                    return ResponseEntity.badRequest().body(response);
                }
                if (!reporteExistente.getEstadoReporteId().equals(pendiente.getId())) {
                    String nombreEstado = estadoReporteRepository.findById(reporteExistente.getEstadoReporteId())
                        .map(EstadoReporte::getNombre)
                        .orElse("desconocido");
                    response.put("error", "El reporte no puede editarse porque su estado es '" + nombreEstado + "'.");
                    return ResponseEntity.badRequest().body(response);
}
                reporteExistente.setTitulo(titulo);
                reporteExistente.setDescripcion(descripcion);
                reporteExistente.setDireccion(direccion);
                reporteExistente.setBarrioId(barrio.getId());
                reporteExistente.setTipoReporteId(tipo.getId());        

                if (imagenes != null && !imagenes.isEmpty() && !imagenes.get(0).isEmpty()) {
                    s3Service.eliminarImagenes(reporteExistente.getImagenes());
                    reporteExistente.setImagenes(s3Service.subirImagenes(imagenes));
                }

                reporteExistente.preActualizar();
                reporteService.guardarReporte(reporteExistente);
                response.put("message", "Reporte actualizado correctamente.");

            } else {
                Reporte reporte = new Reporte();
                reporte.setTitulo(titulo);
                reporte.setDescripcion(descripcion);
                reporte.setDireccion(direccion);
                reporte.setBarrioId(barrio.getId());
                reporte.setTipoReporteId(tipo.getId());
                reporte.setEstadoReporteId(pendiente.getId());              
                reporte.setUsuario(usuario);

                if (imagenes != null && !imagenes.isEmpty() && !imagenes.get(0).isEmpty()) {
                    reporte.setImagenes(s3Service.subirImagenes(imagenes));
                }

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
            EstadoReporte pendiente = estadoReporteRepository.findByNombre("Pendiente").orElseThrow();
            if (!reporte.getEstadoReporteId().equals(pendiente.getId())) {
                String nombreEstado = estadoReporteRepository.findById(reporte.getEstadoReporteId())
                    .map(EstadoReporte::getNombre)
                    .orElse("desconocido");
                response.put("error", "Este reporte ya está en estado '" + nombreEstado + "' y no puede ser eliminado.");
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