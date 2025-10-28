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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("isAdminPrincipal", "adminMain@gmail.com".equals(usuario.getEmail()));
        model.addAttribute("reportes", reporteRepository.findAll());

        // Asegurar que siempre haya un objeto limpio si no viene de flash
        if (!model.containsAttribute("nuevoAdmin")) {
            model.addAttribute("nuevoAdmin", new Usuario());
        }

        // No pasar successMessage aquí
        return "admin_inicio";
    }

    @PostMapping("/cambiar-estado")
    public String cambiarEstado(@RequestParam Long reporteId, @RequestParam String nuevoEstado, HttpSession session, RedirectAttributes redirectAttrs) {
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Reporte reporte = reporteRepository.findById(reporteId).orElse(null);
        if (reporte == null) {
            redirectAttrs.addFlashAttribute("error", "Reporte no encontrado.");
            return "redirect:/admin/inicio";
        }

        // Si el reporte ya está RESUELTO, no permitir cambios
        if (reporte.getEstado() == EstadoReporte.RESUELTO) {
            redirectAttrs.addFlashAttribute("error", "No se puede modificar un reporte ya resuelto.");
            return "redirect:/admin/inicio";
        }

        // Convertir el String a Enum EstadoReporte
        EstadoReporte estado;
        try {
            estado = EstadoReporte.valueOf(nuevoEstado.toUpperCase()); // Asegurar que sea mayúscula
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", "Estado inválido: " + nuevoEstado);
            return "redirect:/admin/inicio";
        }

        reporte.setEstado(estado);
        reporte.setUsuarioAdmin(admin);
        reporte.setFechaModificacion(LocalDateTime.now());
        
        // Forzar la persistencia
        Reporte reporteGuardado = reporteRepository.save(reporte);
        System.out.println("Reporte actualizado - ID: " + reporteGuardado.getId() + ", Nuevo Estado: " + reporteGuardado.getEstado());

        // Pasar el mensaje solo para el cambio de estado
        redirectAttrs.addFlashAttribute("successMessage", "Estado del reporte cambiado correctamente a " + estado.name());
        return "redirect:/admin/inicio";
    }

    @GetMapping("/eliminar-reporte/{id}")
    public String eliminarReporte(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttrs) {
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) {
            redirectAttrs.addFlashAttribute("error", "Reporte no encontrado.");
            return "redirect:/admin/inicio";
        }

        reporteRepository.delete(reporte);
        redirectAttrs.addFlashAttribute("success", "Reporte eliminado correctamente.");
        return "redirect:/admin/inicio";
    }

    @PostMapping("/registrar-admin")
    public ResponseEntity<Map<String, String>> registrarAdmin(
            @ModelAttribute Usuario nuevoAdmin, 
            HttpSession session, 
            RedirectAttributes redirectAttrs) {

        Map<String, String> response = new HashMap<>();
        Usuario adminPrincipal = (Usuario) session.getAttribute("usuarioLogueado");

        // Verificación de permisos
        if (adminPrincipal == null || !adminPrincipal.getEmail().equals("adminMain@gmail.com")) {
            response.put("error", "Solo el administrador principal puede registrar nuevos administradores.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Validación de datos
        if (nuevoAdmin.getNombre() == null || nuevoAdmin.getEmail() == null || nuevoAdmin.getPassword() == null) {
            response.put("error", "Todos los campos son obligatorios.");
            return ResponseEntity.badRequest().body(response);
        }

        // Verificar si el email ya existe
        if (usuarioRepository.findByEmail(nuevoAdmin.getEmail()) != null) {
            response.put("error", "El correo electrónico ya está en uso.");
            return ResponseEntity.badRequest().body(response);
        }

        // Configurar y guardar el nuevo admin (sin encriptar)
        nuevoAdmin.setRol(Rol.ADMIN);
        nuevoAdmin.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(nuevoAdmin);

        // Limpiar el objeto del formulario para el próximo registro
        redirectAttrs.addFlashAttribute("nuevoAdmin", new Usuario());

        response.put("success", "Administrador registrado correctamente.");
        return ResponseEntity.ok(response);
    }

    // Nueva ruta para mostrar el formulario de registro (GET)
    @GetMapping("/registro")
    public String mostrarRegistroCiudadano(Model model, HttpSession session) {
        // Verificar si ya está logueado
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/admin/inicio"; // Redirigir a admin si ya está logueado
        }
        model.addAttribute("usuario", new Usuario()); // Ajuste para coincidir con th:object="${usuario}"
        System.out.println("Accediendo a /registro - Usuario logueado: " + (session.getAttribute("usuarioLogueado") != null) + " - Hora: " + LocalDateTime.now());
        return "registro"; // Usa la vista existente
    }

    // Método para procesar el registro de ciudadanos (POST)
    @PostMapping("/registro")
    public String registrarCiudadano(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttrs) {
        try {
            // Verificar si el email ya existe
            if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
                redirectAttrs.addFlashAttribute("error", "El correo electrónico ya está en uso.");
                return "redirect:/registro";
            }

            // Validación básica
            if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty() ||
                usuario.getEmail() == null || usuario.getEmail().trim().isEmpty() ||
                usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Todos los campos son obligatorios.");
                return "redirect:/registro";
            }

            // Configurar y guardar el nuevo ciudadano
            usuario.setRol(Rol.CIUDADANO); // Asumiendo que tienes Rol.CIUDADANO
            usuario.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(usuario);
            System.out.println("Ciudadano registrado - Email: " + usuario.getEmail() + " - Hora: " + LocalDateTime.now());

            // Añadir modal de éxito
            redirectAttrs.addFlashAttribute("modalSuccess", true);
            redirectAttrs.addFlashAttribute("modalMessage", "Registro exitoso");
            return "redirect:/registro"; // Vuelve a la misma página para mostrar el modal
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
            System.out.println("Error al registrar ciudadano: " + e.getMessage() + " - Hora: " + LocalDateTime.now());
            return "redirect:/registro";
        }
    }

    // Nuevo método para capturar errores o redirecciones no manejadas
    @ExceptionHandler(Exception.class)
    public String handleError(Exception ex, RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "Ocurrió un error inesperado: " + ex.getMessage());
        System.out.println("Error capturado: " + ex.getMessage() + " - Hora: " + LocalDateTime.now());
        return "redirect:/registro";
    }

    // Método de depuración para rastrear solicitudes
    @GetMapping("/debug")
    public String debugRequest(HttpSession session) {
        System.out.println("Solicitud recibida - Ruta: /debug, Usuario logueado: " + (session.getAttribute("usuarioLogueado") != null) + " - Hora: " + LocalDateTime.now());
        return "redirect:/login";
    }

    // Nuevo método para rastrear solicitudes a rutas no definidas
    @GetMapping("/track")
    public String trackRequest(HttpSession session) {
        System.out.println("Solicitud rastreada - Ruta: /track, Usuario logueado: " + (session.getAttribute("usuarioLogueado") != null) + " - Hora: " + LocalDateTime.now());
        return "redirect:/login";
    }
}