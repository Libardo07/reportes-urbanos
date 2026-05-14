package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import com.reportes.urbanos.reportes_api.repository.EstadoReporteRepository;
import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import com.reportes.urbanos.reportes_api.repository.ReporteRepositoryCustom;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;
import com.reportes.urbanos.reportes_api.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    private EstadoReporteRepository estadoReporteRepository;

    @Autowired
    private BarrioService barrioService;

    @Autowired
    private TipoReporteService tipoReporteService;

    @Autowired
    private EstadoReporteService estadoReporteService;

    @Autowired
    private ReporteRepositoryCustom reporteRepositoryCustom;



    @Value("${admin.email}")
    private String adminEmail;

    @ModelAttribute
    public void addCatalogMaps(Model model) {
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

    // Método utilitario para obtener el usuario logueado desde Spring Security
    private Usuario getUsuarioLogueado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.getUsuarioPorEmail(email);
    }

    @GetMapping("/inicio")
    public String mostrarPanelAdmin(Model model) {
        Usuario usuario = getUsuarioLogueado();
        model.addAttribute("usuario", usuario);
        model.addAttribute("reportes", reporteService.getReportesAdmin());
        return "admin_inicio";
    }

    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(Model model) {
        model.addAttribute("reportes", reporteService.getReportesAdmin());
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

            EstadoReporte resuelto = estadoReporteRepository.findByNombre("Resuelto").orElseThrow();
            if (reporte.getEstadoReporteId().equals(resuelto.getId())) {
                response.put("error", "No se puede modificar un reporte ya resuelto.");
                return ResponseEntity.badRequest().body(response);
            }

            EstadoReporte nuevoEstadoObj = estadoReporteRepository
                    .findByNombre(nuevoEstado)
                    .orElse(null);
            if (nuevoEstadoObj == null) {
                response.put("error", "Estado inválido.");
                return ResponseEntity.badRequest().body(response);
            }

            reporte.setEstadoReporteId(nuevoEstadoObj.getId());
            reporte.setUsuarioAdmin(admin);
            reporte.preActualizar();
            reporteService.guardarReporte(reporte);
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

        List<Reporte> reportes = reporteRepositoryCustom.filtrar(
            estadoId, tipoId, barrioId, nombreUsuario, desde, hasta);

        model.addAttribute("reportes", reportes);
        return "admin/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping("/barrios-buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarBarrios(@RequestParam String q) {
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
}

