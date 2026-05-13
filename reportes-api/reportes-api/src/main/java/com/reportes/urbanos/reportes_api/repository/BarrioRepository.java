package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {
    Optional<Barrio> findByNombre(String nombre);
}