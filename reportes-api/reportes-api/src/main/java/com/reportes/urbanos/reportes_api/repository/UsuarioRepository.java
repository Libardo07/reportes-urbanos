package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByEmail(String email);
}
