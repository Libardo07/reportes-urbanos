package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import com.reportes.urbanos.reportes_api.repository.TipoReporteRepository;
import com.reportes.urbanos.reportes_api.repository.EstadoReporteRepository;
import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.CatalogoService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    private TipoReporteRepository tipoReporteRepository;

    @Autowired
    private EstadoReporteRepository estadoReporteRepository;

    @Autowired
    private CatalogoService catalogoService;

    @Value("${admin.email}")
    private String adminEmail;

    @ModelAttribute
    public void populateModelsWithCommonData(Model model) {
        model.addAttribute("barrios", catalogoService.getBarriosMap().entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .collect(Collectors.toList()));
        model.addAttribute("tipos",      tipoReporteRepository.findAll());
        model.addAttribute("estadosMap", catalogoService.getEstadosMap());
        model.addAttribute("tiposMap",   catalogoService.getTiposMap());
        model.addAttribute("barriosMap", catalogoService.getBarriosMap());
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
}