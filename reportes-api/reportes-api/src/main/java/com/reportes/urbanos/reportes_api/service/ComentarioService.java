package com.reportes.urbanos.reportes_api.service;

import com.reportes.urbanos.reportes_api.entity.Comentario;
import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.ComentarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    // Obtiene los comentarios principales de un reporte (sin respuestas) ordenados por fecha
    public List<Comentario> getComentariosPorReporte(String reporteId) {
        return comentarioRepository
            .findByReporteIdAndParentIdIsNullOrderByFechaCreacionDesc(reporteId);
    }

    // Obtiene las respuestas de un comentario ordenadas por fecha
    public List<Comentario> getRespuestasPorComentario(String parentId) {
        return comentarioRepository.findByParentIdOrderByFechaCreacionAsc(parentId);
    }

    // Crea y guarda un comentario principal en un reporte, validando que el texto no esté vacío ni exceda el límite permitido
    public Comentario agregarComentario(String texto, Usuario usuario, String reporteId) {
        if (texto == null || texto.trim().isBlank())
            throw new IllegalArgumentException("El comentario no puede estar vacío.");
        if (texto.trim().length() > 500)
            throw new IllegalArgumentException("Máximo 500 caracteres.");
        Comentario c = new Comentario(texto.trim(), usuario, reporteId);
        c.setUsuarioId(usuario.getId());
        c.setUsuarioNombre(usuario.getNombre());
        return comentarioRepository.save(c);
    }

    // Crea y guarda una respuesta a un comentario en un reporte, validando el contenido del texto
    public Comentario responderComentario(String texto, Usuario usuario, String reporteId, String parentId) {
        if (texto == null || texto.trim().isBlank())
            throw new IllegalArgumentException("La respuesta no puede estar vacía.");
        if (texto.trim().length() > 500)
            throw new IllegalArgumentException("Máximo 500 caracteres.");
        Comentario c = new Comentario(texto.trim(), usuario, reporteId);
        c.setParentId(parentId);
        return comentarioRepository.save(c);
    }

    // Elimina un comentario y también sus respuestas asociadas (si existen)
    public void eliminarComentario(String comentarioId) {
    List<Comentario> respuestas = comentarioRepository.findByParentId(comentarioId);
    if (respuestas != null && !respuestas.isEmpty()) {
        comentarioRepository.deleteAll(respuestas);
    }
    comentarioRepository.deleteById(comentarioId);
    }

    // Cuenta la cantidad total de comentarios asociados a un reporte
    public long contarComentarios(String reporteId) {
        return comentarioRepository.countByReporteId(reporteId);
    }
}
