package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ReporteRepository reporteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/inicio")
    public String mostrarPanelAdmin(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("reportes", reporteRepository.findAll());
        return "admin_inicio";
    }


    @GetMapping(value = "/fragmento/lista-reportes", produces = "text/html")
    public String fragmentoListaReportes(Model model) {
        model.addAttribute("reportes", reporteRepository.findAll());
        return "admin/fragments/lista-reportes :: lista-reportes";
    }

    @GetMapping(value = "/fragmento/formulario-admin", produces = "text/html")
    public String fragmentoFormularioAdmin(Model model) {
        model.addAttribute("nuevoAdmin", new Usuario());
        return "admin/fragments/formulario-admin :: formulario-admin";
    }


    @PostMapping("/registrar-admin")
    public ResponseEntity<Map<String, String>> registrarAdmin(@ModelAttribute Usuario nuevoAdmin, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Usuario adminPrincipal = (Usuario) session.getAttribute("usuarioLogueado");
        if (adminPrincipal == null || !adminPrincipal.getEmail().equals("adminMain@gmail.com")) {
            response.put("error", "Solo el administrador principal puede registrar nuevos administradores.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try {
            if (usuarioRepository.findByEmail(nuevoAdmin.getEmail()) != null) {
                response.put("error", "El correo electrónico ya está en uso.");
                return ResponseEntity.badRequest().body(response);
            }
            nuevoAdmin.setRol(Rol.ADMIN);
            nuevoAdmin.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(nuevoAdmin);
            response.put("success", "true");
            response.put("message", "Administrador registrado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al registrar: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-estado")
    public ResponseEntity<Map<String, String>> cambiarEstado(@RequestParam Long reporteId, @RequestParam String nuevoEstado, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            response.put("error", "Usuario no autorizado.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try {
            Reporte reporte = reporteRepository.findById(reporteId).orElse(null);
            if (reporte == null) {
                response.put("error", "Reporte no encontrado.");
                return ResponseEntity.badRequest().body(response);
            }
            if (reporte.getEstado() == EstadoReporte.RESUELTO) {
                response.put("error", "No se puede modificar un reporte ya resuelto.");
                return ResponseEntity.badRequest().body(response);
            }
            EstadoReporte estado;
            try {
                estado = EstadoReporte.valueOf(nuevoEstado.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("error", "Estado inválido.");
                return ResponseEntity.badRequest().body(response);
            }
            reporte.setEstado(estado);
            reporte.setUsuarioAdmin(admin);
            reporteRepository.save(reporte);
            response.put("success", "true");
            response.put("message", "Estado del reporte cambiado correctamente.");
        } catch (Exception e) {
            response.put("error", "Error al cambiar estado: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/eliminar-reporte/{id}")
    public ResponseEntity<Map<String, String>> eliminarReporte(@PathVariable Long id, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            response.put("error", "Usuario no autorizado.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try {
            Reporte reporte = reporteRepository.findById(id).orElse(null);
            if (reporte == null) {
                response.put("error", "Reporte no encontrado.");
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