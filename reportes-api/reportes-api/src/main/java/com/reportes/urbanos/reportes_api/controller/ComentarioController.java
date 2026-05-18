package com.reportes.urbanos.reportes_api.controller;

import com.reportes.urbanos.reportes_api.entity.Comentario;
import com.reportes.urbanos.reportes_api.entity.Reporte;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.ReporteRepository;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import com.reportes.urbanos.reportes_api.service.BarrioService;
import com.reportes.urbanos.reportes_api.service.ComentarioService;
import com.reportes.urbanos.reportes_api.service.EstadoReporteService;
import com.reportes.urbanos.reportes_api.service.ReporteService;
import com.reportes.urbanos.reportes_api.service.TipoReporteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.reportes.urbanos.reportes_api.entity.Barrio;
import com.reportes.urbanos.reportes_api.entity.EstadoReporte;
import com.reportes.urbanos.reportes_api.entity.TipoReporte;
import java.util.stream.Collectors;


import java.util.List;
import java.util.Map;

@Controller
public class ComentarioController {

    @Autowired 
    private ComentarioService comentarioService;

    @Autowired 
    private ReporteRepository reporteRepository;

    @Autowired 
    private ReporteService reporteService;

    @Autowired 
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BarrioService barrioService;

    @Autowired
    private TipoReporteService tipoReporteService;

    @Autowired
    private EstadoReporteService estadoReporteService;
;



    private Usuario getUsuarioLogueado() {
        try {
            String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            if (email == null || email.equals("anonymousUser")) return null;
            return usuarioRepository.findByEmail(email);
        } catch (Exception e) { return null; }
    }

    private void addCatalogMaps(Model model) {
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


    // ── Fragmento: lista explorar reportes ──────────────────────────────────
    @GetMapping(value = "/reportes/fragmento/explorar", produces = "text/html")
    public String explorarReportes(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        org.springframework.data.domain.Page<Reporte> pageResult =
            reporteService.explorarReportesPaginado(q, page);

        List<Reporte> reportesPagina = pageResult.getContent();

        Map<String, Long> contadores = new java.util.HashMap<>();
        reportesPagina.forEach(r ->
            contadores.put(r.getId(), comentarioService.contarComentarios(r.getId()))
        );

        model.addAttribute("reportes",   reportesPagina);
        model.addAttribute("contadores", contadores);
        model.addAttribute("q",          q);
        model.addAttribute("hayMas",     pageResult.hasNext());
        model.addAttribute("nextPage",   page + 1);
        model.addAttribute("total",      pageResult.getTotalElements());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pageResult.getTotalPages());
        model.addAttribute("hayAnterior", pageResult.hasPrevious());
        model.addAttribute("haySiguiente", pageResult.hasNext());
        addCatalogMaps(model);
        return "fragmentos/explorar_reportes :: explorar-reportes";
    }

    // ── Fragmento: detalle de un reporte con comentarios ────────────────────
    @GetMapping(value = "/reportes/fragmento/detalle/{id}", produces = "text/html")
    public String detalleReporte(@PathVariable String id, Model model) {
        Reporte reporte = reporteRepository.findById(id).orElse(null);
        if (reporte == null) return "redirect:/";

        Usuario usuario = getUsuarioLogueado();
        List<Comentario> comentarios = comentarioService.getComentariosPorReporte(id);

        Map<String, List<Comentario>> respuestas = new java.util.HashMap<>();
        comentarios.forEach(c ->
            respuestas.put(c.getId(), comentarioService.getRespuestasPorComentario(c.getId()))
            
        );

        model.addAttribute("reporte",     reporte);
        model.addAttribute("comentarios", comentarios !=null ? comentarios : List.of());
        model.addAttribute("respuestas", respuestas);
        model.addAttribute("usuario",     usuario);
        model.addAttribute("esAdmin",     usuario != null && usuario.getRol() == Rol.ADMIN);
        model.addAttribute("total",       comentarioService.contarComentarios(id));
        addCatalogMaps(model);
        return "fragmentos/detalle_reporte :: detalle-reporte";
    }

    
    @PostMapping("/reporte/{id}/responder/{parentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> responder(
            @PathVariable String id,
            @PathVariable String parentId,
            @RequestParam String texto) {

        Usuario usuario = getUsuarioLogueado();
        if (usuario == null)
            return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión."));

        try {
            Comentario c = comentarioService.responderComentario(texto, usuario, id, parentId);
            return ResponseEntity.ok(Map.of(
            "success", true,
            "id",      c.getId(),
            "texto",   c.getTexto(),
            "nombre",  c.getUsuarioNombre(),
            "inicial", c.getUsuarioNombre().substring(0, 1).toUpperCase(),
            "fecha",   c.getFechaCreacion().toString()
        ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

        // ── Agregar comentario raíz ──────────────────────────────────────────────
    @PostMapping("/reporte/{id}/comentar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comentar(
            @PathVariable String id,
            @RequestParam String texto) {

        Usuario usuario = getUsuarioLogueado();
        if (usuario == null)
            return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión."));

        try {
            Comentario c = comentarioService.agregarComentario(texto, usuario, id);
            return ResponseEntity.ok(Map.of(
            "success", true,
            "id",      c.getId(),
            "texto",   c.getTexto(),
            "nombre",  c.getUsuarioNombre(),
            "inicial", c.getUsuarioNombre().substring(0, 1).toUpperCase(),
            "fecha",   c.getFechaCreacion().toString()
        ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Eliminar comentario (solo admin) ─────────────────────────────────────
    @PostMapping("/admin/eliminar-comentario/{comentarioId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarComentario(
            @PathVariable String comentarioId) {

        Usuario admin = getUsuarioLogueado();
        if (admin == null || admin.getRol() != Rol.ADMIN)
            return ResponseEntity.status(403).body(Map.of("error", "Sin permisos."));

        try {
            comentarioService.eliminarComentario(comentarioId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}