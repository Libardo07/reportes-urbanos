package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.EstadoReporteRepository;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import com.reportes.urbanos.reportes_api.repository.ReporteRepositoryCustom;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.EmailService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;
import com.reportes.urbanos.reportes_api.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.aggregation.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import com.reportes.urbanos.reportes_api.entity.TipoReporte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private ReporteRepository reporteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private BarrioService barrioService;

    @Autowired
    private TipoReporteService tipoReporteService;

    @Autowired
    private EstadoReporteService estadoReporteService;

    @Autowired
    private ReporteRepositoryCustom reporteRepositoryCustom;

    @Autowired
    private EstadoReporteRepository estadoReporteRepository; 

    @Autowired
    private EmailService emailService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${admin.email}")
    private String adminEmail;

    @ModelAttribute
    public void addCatalogMaps(Model model) {
        // Todos usan caché — no van a MySQL
        List<Barrio> barrios = barrioService.getBarriosOrdenados();
        List<TipoReporte> tipos = tipoReporteService.getTipos();
        List<EstadoReporte> estados = estadoReporteService.getEstados();

        model.addAttribute("estadosMap",
            estados.stream().collect(Collectors.toMap(EstadoReporte::getId, EstadoReporte::getNombre)));
        model.addAttribute("tiposMap",
            tipos.stream().collect(Collectors.toMap(TipoReporte::getId, TipoReporte::getNombre)));
        model.addAttribute("barriosMap",
            barrios.stream().collect(Collectors.toMap(Barrio::getId, Barrio::getNombre)));
    }

    private Usuario getUsuarioLogueado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.getUsuarioPorEmail(email);
    }

    @GetMapping("/inicio")
    public String mostrarPanelAdmin(Model model) {
        Usuario usuario = getUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        return "admin_inicio";
    }

    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        var pagina = reporteService.getReportesAdminPaginado(page);
        model.addAttribute("reportes", pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("hayAnterior", pagina.hasPrevious());
        model.addAttribute("haySiguiente", pagina.hasNext());
        return "admin/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping(value = "/fragmento/formulario-admin", produces = "text/html")
    public String fragmentoFormularioAdmin(Model model) {
        model.addAttribute("nuevoAdmin", new Usuario());
        return "admin/fragments/formulario-admin :: formulario-admin";
    }

    @PostMapping("/registrar-admin")
    public ResponseEntity<Map<String, String>> registrarAdmin(@ModelAttribute Usuario nuevoAdmin) {
        Map<String, String> response = new HashMap<>();
        Usuario adminPrincipal = getUsuarioLogueado();
        if (adminPrincipal == null || !adminPrincipal.getEmail().equals(adminEmail)) {
            response.put("error", "Solo el administrador principal puede registrar nuevos administradores.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try {
            if (usuarioRepository.findByEmail(nuevoAdmin.getEmail()) != null) {
                response.put("error", "El correo electrónico ya está en uso.");
                return ResponseEntity.badRequest().body(response);
            }
            nuevoAdmin.setRol(Rol.ADMIN);
            nuevoAdmin.setFechaCreacion(LocalDateTime.now(ZoneId.of("America/Bogota")));
            nuevoAdmin.setPassword(passwordEncoder.encode(nuevoAdmin.getPassword()));
            nuevoAdmin.setVerificado(true);
            usuarioRepository.save(nuevoAdmin);
            response.put("success", "true");
            response.put("message", "Administrador registrado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al registrar: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-estado")
    public ResponseEntity<Map<String, String>> cambiarEstado(@RequestParam String reporteId,
                                                            @RequestParam String nuevoEstado) {
        Map<String, String> response = new HashMap<>();
        Usuario admin = getUsuarioLogueado();
        try {
            Reporte reporte = reporteRepository.findById(reporteId).orElse(null);
            if (reporte == null) {
                response.put("error", "Reporte no encontrado.");
                return ResponseEntity.badRequest().body(response);
            }

            EstadoReporte resuelto   = estadoReporteService.getByNombre("Resuelto");

            if (reporte.getEstadoReporteId().equals(resuelto.getId())) {
                response.put("error", "No se puede modificar un reporte ya resuelto.");
                return ResponseEntity.badRequest().body(response);
            }

            EstadoReporte nuevoEstadoObj = estadoReporteService.getByNombre(nuevoEstado);

            reporte.setEstadoReporteId(nuevoEstadoObj.getId());
            reporte.setUsuarioAdmin(admin);
            reporte.preActualizar();
            reporteService.guardarReporte(reporte);

            // Enviar correo según el nuevo estado
            Usuario usuarioReporte = reporte.getUsuario();
            System.out.println("=== NUEVO ESTADO: '" + nuevoEstado + "'");
            System.out.println("=== USUARIO EMAIL: " + (usuarioReporte != null ? usuarioReporte.getEmail() : "NULL"));
            if (usuarioReporte != null && usuarioReporte.getEmail() != null) {
                if ("En_Proceso".equals(nuevoEstado)) {
                    emailService.enviarCorreoEnProceso(
                        usuarioReporte.getEmail(),
                        usuarioReporte.getNombre(),
                        reporte.getTitulo(),
                        admin.getNombre()
                    );
                } else if ("Resuelto".equals(nuevoEstado)) {
                    emailService.enviarCorreoResuelto(
                        usuarioReporte.getEmail(),
                        usuarioReporte.getNombre(),
                        reporte.getTitulo(),
                        reporte.getId()
                    );
                }
            }

            response.put("success", "true");
            response.put("message", "Estado del reporte cambiado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al cambiar estado: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/eliminar-reporte/{id}")
    public ResponseEntity<Map<String, String>> eliminarReporte(@PathVariable String id) {
        Map<String, String> response = new HashMap<>();
        try {
            Reporte reporte = reporteRepository.findById(id).orElse(null);
            if (reporte == null) {
                response.put("error", "Reporte no encontrado.");
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

    @GetMapping(value = "/fragmento/filtrar-reportes", produces = "text/html")
    public String filtrarReportes(
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) Long barrioId,
            @RequestParam(required = false) String nombreUsuario,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String horaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String horaHasta,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime desde = null;
        LocalDateTime hasta = null;

        try {
            if (fechaDesde != null && !fechaDesde.isBlank()) {
                LocalTime hora = (horaDesde != null && !horaDesde.isBlank())
                    ? LocalTime.parse(horaDesde) : LocalTime.MIN;
                desde = LocalDate.parse(fechaDesde).atTime(hora);
            }
            if (fechaHasta != null && !fechaHasta.isBlank()) {
                LocalTime hora = (horaHasta != null && !horaHasta.isBlank())
                    ? LocalTime.parse(horaHasta) : LocalTime.MAX;
                hasta = LocalDate.parse(fechaHasta).atTime(hora);
            }
        } catch (Exception ignored) {}

        var pagina = reporteRepositoryCustom.filtrar(
            estadoId, tipoId, barrioId, nombreUsuario, desde, hasta, page);

        model.addAttribute("reportes", pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("hayAnterior", pagina.hasPrevious());
        model.addAttribute("haySiguiente", pagina.hasNext());
        return "admin/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping("/barrios-buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarBarrios(@RequestParam String q) {
        //  Usa el caché de barrios en lugar de ir a MySQL
        return barrioService.getBarriosOrdenados().stream()
            .filter(b -> b.getNombre().toLowerCase().contains(q.toLowerCase()))
            .map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", b.getId());
                map.put("nombre", b.getNombre());
                return map;
            })
            .collect(Collectors.toList());
    }

    @GetMapping(value = "/fragmento/inicio", produces = "text/html")
    public String fragmentoInicio(Model model) {

        // Conteos directos en MongoDB sin cargar documentos
        long totalReportes = reporteRepository.count();

        // IDs de estados desde MySQL
        EstadoReporte pendiente   = estadoReporteRepository.findByNombre("Pendiente").orElse(null);
        EstadoReporte enProceso  = estadoReporteRepository.findByNombre("En_Proceso").orElse(null);
        EstadoReporte resuelto    = estadoReporteRepository.findByNombre("Resuelto").orElse(null);

        long totalPendientes  = pendiente  != null ? reporteRepository.countByEstadoReporteId(pendiente.getId())  : 0;
        long totalEnProgreso  = enProceso != null ? reporteRepository.countByEstadoReporteId(enProceso.getId()) : 0;
        long totalResueltos   = resuelto   != null ? reporteRepository.countByEstadoReporteId(resuelto.getId())   : 0;

        // IDs de tipos desde MySQL
        TipoReporte infraestructura = tipoReporteService.getTipos().stream()
            .filter(t -> t.getNombre().equalsIgnoreCase("Infraestructura")).findFirst().orElse(null);
        TipoReporte servicios = tipoReporteService.getTipos().stream()
            .filter(t -> t.getNombre().toLowerCase().contains("servicio")).findFirst().orElse(null);

        long totalInfraestructura = infraestructura != null ? reporteRepository.countByTipoReporteId(infraestructura.getId()) : 0;
        long totalServicios       = servicios       != null ? reporteRepository.countByTipoReporteId(servicios.getId())       : 0;

        // Usuarios y admins
        long totalUsuarios = usuarioRepository.countByRol(Rol.CIUDADANO);
        long totalAdmins   = usuarioRepository.countByRol(Rol.ADMIN);

        // Barrio con más reportes usando aggregation
        Map<Long, String> barriosMap = barrioService.getBarriosOrdenados().stream()
            .collect(Collectors.toMap(Barrio::getId, Barrio::getNombre));

        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("barrioId").ne(null)),
            Aggregation.group("barrioId").count().as("total"),
            Aggregation.sort(Sort.Direction.DESC, "total"),
            Aggregation.limit(1)
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
            agg, "reportes", Document.class);

        String barrioMasReportes = "-";
        Document topDoc = results.getUniqueMappedResult();
        if (topDoc != null) {
            Long topBarrioId = ((Number) topDoc.get("_id")).longValue();
            long count = ((Number) topDoc.get("total")).longValue();
            String nombre = barriosMap.getOrDefault(topBarrioId, "Desconocido");
            barrioMasReportes = nombre + " - " + count + " reportes";
        }

        model.addAttribute("totalReportes",      totalReportes);
        model.addAttribute("totalPendientes",    totalPendientes);
        model.addAttribute("totalEnProgreso",    totalEnProgreso);
        model.addAttribute("totalResueltos",     totalResueltos);
        model.addAttribute("totalInfraestructura", totalInfraestructura);
        model.addAttribute("totalServicios",     totalServicios);
        model.addAttribute("totalUsuarios",      totalUsuarios);
        model.addAttribute("totalAdmins",        totalAdmins);
        model.addAttribute("barrioMasReportes",  barrioMasReportes);

        return "admin/fragments/inicio-admin :: inicio-admin";
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        return "admin/fragments/dashboard :: dashboard";
    }
}