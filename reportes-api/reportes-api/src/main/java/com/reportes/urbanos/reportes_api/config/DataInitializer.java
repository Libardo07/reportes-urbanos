package com.reportes.urbanos.reportes_api.config;

import com.reportes.urbanos.reportes_api.entity.Usuario;
import com.reportes.urbanos.reportes_api.enums.Rol;
import com.reportes.urbanos.reportes_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository == null) {
            System.err.println("Error: UsuarioRepository no ha sido inyectado.");
            return;
        }
        // Crear admin principal si no existe
        if (usuarioRepository.findByEmail("adminMain@gmail.com") == null) {
            Usuario adminPrincipal = new Usuario();
            adminPrincipal.setNombre("adminMain");
            adminPrincipal.setEmail("adminMain@gmail.com");
            adminPrincipal.setPassword("adminMain");
            adminPrincipal.setRol(Rol.ADMIN);
            adminPrincipal.setFechaCreacion(LocalDateTime.now());
            try {
                usuarioRepository.save(adminPrincipal);
                System.out.println("Admin principal creado exitosamente.");
            } catch (Exception e) {
                System.err.println("Error al guardar el admin principal: " + e.getMessage());
            }
        }
    }
}