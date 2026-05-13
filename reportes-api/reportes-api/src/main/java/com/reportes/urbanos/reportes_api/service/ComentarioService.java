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

    // Solo retorna comentarios raíz
    public List<Comentario> getComentariosPorReporte(String reporteId) {
        return comentarioRepository
            .findByReporteIdAndParentIdIsNullOrderByFechaCreacionDesc(reporteId);
    }

    // Retorna las respuestas de un comentario específico
    public List<Comentario> getRespuestasPorComentario(String parentId) {
        return comentarioRepository.findByParentIdOrderByFechaCreacionAsc(parentId);
    }

    public Comentario agregarComentario(String texto, Usuario usuario, String reporteId) {
        if (texto == null || texto.trim().isBlank())
            throw new IllegalArgumentException("El comentario no puede estar vacío.");
        if (texto.trim().length() > 500)
            throw new IllegalArgumentException("Máximo 500 caracteres.");
        return comentarioRepository.save(new Comentario(texto.trim(), usuario, reporteId));
    }

    public Comentario responderComentario(String texto, Usuario usuario, String reporteId, String parentId) {
        if (texto == null || texto.trim().isBlank())
            throw new IllegalArgumentException("La respuesta no puede estar vacía.");
        if (texto.trim().length() > 500)
            throw new IllegalArgumentException("Máximo 500 caracteres.");
        Comentario c = new Comentario(texto.trim(), usuario, reporteId);
        c.setParentId(parentId);
        return comentarioRepository.save(c);
    }

    public void eliminarComentario(String comentarioId) {
    // Eliminar todas las respuestas asociadas primero
    List<Comentario> respuestas = comentarioRepository.findByParentId(comentarioId);
    if (respuestas != null && !respuestas.isEmpty()) {
        comentarioRepository.deleteAll(respuestas);
    }
    // Eliminar el comentario principal
    comentarioRepository.deleteById(comentarioId);
}

    public long contarComentarios(String reporteId) {
        return comentarioRepository.countByReporteId(reporteId);
    }
}
