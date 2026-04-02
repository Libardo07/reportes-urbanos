package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Cacheable(value = "usuarios", key = "#email")
    public Usuario getUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @CacheEvict(value = "usuarios", key = "#email")
    public void invalidarCacheUsuario(String email) {
        // Solo invalida el caché cuando sea necesario
    }
}
