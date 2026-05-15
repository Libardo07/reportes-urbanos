package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.repository.*;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
import com.reportes.urbanos.reportes_api.service.S3Service;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;
import com.reportes.urbanos.reportes_api.service.UsuarioService;
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
    private UsuarioService usuarioService;

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

    private Usuario getUsuarioLogueado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.getUsuarioPorEmail(email);
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

    // ← CAMBIADO: ahora usa paginación
    @GetMapping("/inicio")
    public String inicioCiudadano(Model model) {
        Usuario usuario = getUsuarioLogueado();
        var pagina = reporteService.getReportesUsuarioPaginado(usuario, 0);
        model.addAttribute("usuario", usuario);
        model.addAttribute("reporte", new Reporte());
        model.addAttribute("reportes", pagina.getContent());
        model.addAttribute("paginaActual", 0);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("hayAnterior", pagina.hasPrevious());
        model.addAttribute("haySiguiente", pagina.hasNext());
        return "usuario_inicio";
    }

    // ← CAMBIADO: ahora recibe ?page= y pagina
    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Usuario usuario = getUsuarioLogueado();
        var pagina = reporteService.getReportesUsuarioPaginado(usuario, page);
        model.addAttribute("reportes", pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("hayAnterior", pagina.hasPrevious());
        model.addAttribute("haySiguiente", pagina.hasNext());
        return "usuario/fragments/lista-reportes :: lista-reportes";
    }

    // sin cambios desde aquí
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
    

    @GetMapping(value = "/fragmento/inicio", produces = "text/html")
    public String fragmentoInicio(Model model) {
        Usuario usuario = getUsuarioLogueado();
        List<Reporte> todos = reporteService.getReportesUsuario(usuario);

        Map<Long, String> estados = estadoReporteService.getEstados().stream()
            .collect(Collectors.toMap(EstadoReporte::getId, EstadoReporte::getNombre));
        Map<Long, String> tipos = tipoReporteService.getTipos().stream()
            .collect(Collectors.toMap(TipoReporte::getId, TipoReporte::getNombre));

        long totalReportes      = todos.size();
        long totalPendientes    = todos.stream().filter(r -> "Pendiente".equals(estados.get(r.getEstadoReporteId()))).count();
        long totalEnProgreso    = todos.stream().filter(r -> "En_Proceso".equals(estados.get(r.getEstadoReporteId()))).count();
        long totalResueltos     = todos.stream().filter(r -> "Resuelto".equals(estados.get(r.getEstadoReporteId()))).count();
        long totalInfraestructura = todos.stream().filter(r -> "Infraestructura".equals(tipos.get(r.getTipoReporteId()))).count();
        long totalServicios     = todos.stream().filter(r -> "Servicios Publicos".equals(tipos.get(r.getTipoReporteId()))).count();

        String ultimoReporte = todos.stream()
            .filter(r -> r.getFechaModificacion() != null)
            .map(r -> r.getFechaModificacion())
            .max(java.time.LocalDateTime::compareTo)
            .map(f -> java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(f))
            .orElse("Sin reportes");

        model.addAttribute("totalReportes", totalReportes);
        model.addAttribute("totalPendientes", totalPendientes);
        model.addAttribute("totalEnProgreso", totalEnProgreso);
        model.addAttribute("totalResueltos", totalResueltos);
        model.addAttribute("totalInfraestructura", totalInfraestructura);
        model.addAttribute("totalServicios", totalServicios);
        model.addAttribute("ultimoReporte", ultimoReporte);

        return "usuario/fragments/inicio-usuario :: inicio-usuario";
    }
}