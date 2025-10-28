package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.*;
import com.reportes.urbanos.reportes_api.enums.EstadoReporte;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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

        List<Reporte> reportes = reporteRepository.findByUsuario(usuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("reporte", new Reporte());
        model.addAttribute("barrios", barrioRepository.findAll());
        model.addAttribute("tipos", tipoRepository.findAll());
        model.addAttribute("reportes", reportes);
        return "usuario_inicio";
    }

    @PostMapping("/guardar-reporte")
    public String guardarReporte(@ModelAttribute Reporte reporte,
                            @RequestParam Long barrioId,
                            @RequestParam Long tipoId,
                            HttpSession session,
                            RedirectAttributes redirectAttrs) {
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
        return "redirect:/login";
    }

    Barrio barrio = barrioRepository.findById(barrioId).orElse(null);
    Tipo tipo = tipoRepository.findById(tipoId).orElse(null);


    if (reporte.getId() != null) {
        Reporte reporteExistente = reporteRepository.findById(reporte.getId()).orElse(null);
        if (reporteExistente == null || !reporteExistente.getUsuario().getId().equals(usuario.getId())) {
            redirectAttrs.addFlashAttribute("error", "No se pudo editar: el reporte no existe o no pertenece a este usuario.");
            return "redirect:/usuario/inicio";
        }


        if (!reporteExistente.getEstado().equals(EstadoReporte.PENDIENTE)) {
            redirectAttrs.addFlashAttribute("error", "El reporte no puede editarse porque su estado es '" + reporteExistente.getEstado() + "'.");
            return "redirect:/usuario/inicio";
        }

        
        reporteExistente.setTitulo(reporte.getTitulo());
        reporteExistente.setDescripcion(reporte.getDescripcion());
        reporteExistente.setDireccion(reporte.getDireccion());
        reporteExistente.setBarrio(barrio);
        reporteExistente.setTipo(tipo);

        reporteRepository.save(reporteExistente);
        redirectAttrs.addFlashAttribute("success", "Reporte actualizado correctamente.");
    } 
    
    else {
        reporte.setBarrio(barrio);
        reporte.setTipo(tipo);
        reporte.setUsuario(usuario);
        reporte.setUsuarioAdmin(null);
        reporteRepository.save(reporte);
        redirectAttrs.addFlashAttribute("success", "Reporte creado correctamente.");
    }

    return "redirect:/usuario/inicio";
}


    @GetMapping("/editar-reporte/{id}")
    public String editarReporte(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttrs) {
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
        return "redirect:/login";
    }

    Reporte reporte = reporteRepository.findById(id).orElse(null);
    if (reporte == null || !reporte.getUsuario().getId().equals(usuario.getId())) {
        redirectAttrs.addFlashAttribute("error", "Reporte no encontrado.");
        return "redirect:/usuario/inicio";
    }

    if (!reporte.getEstado().equals(EstadoReporte.PENDIENTE)) {
        redirectAttrs.addFlashAttribute("error", "Este reporte ya está en estado '" + reporte.getEstado() + "' y no puede ser editado.");
        return "redirect:/usuario/inicio";
    }

    model.addAttribute("reporte", reporte);
    model.addAttribute("barrios", barrioRepository.findAll());
    model.addAttribute("tipos", tipoRepository.findAll());
    return "editar_reporte";
}

    @GetMapping("/eliminar-reporte/{id}")
    public String eliminarReporte(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttrs) {
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    if (usuario == null || usuario.getRol() != Rol.CIUDADANO) {
        return "redirect:/login";
    }

    Reporte reporte = reporteRepository.findById(id).orElse(null);
    if (reporte == null || !reporte.getUsuario().getId().equals(usuario.getId())) {
        redirectAttrs.addFlashAttribute("error", "Reporte no encontrado.");
        return "redirect:/usuario/inicio";
    }

    if (!reporte.getEstado().equals(EstadoReporte.PENDIENTE)) {
        redirectAttrs.addFlashAttribute("error", "Este reporte ya está en estado '" + reporte.getEstado() + "' y no puede ser eliminado.");
        return "redirect:/usuario/inicio";
    }

    reporteRepository.delete(reporte);
    redirectAttrs.addFlashAttribute("success", "Reporte eliminado correctamente.");
    return "redirect:/usuario/inicio";
}


}
