package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.VerificacionToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VerificacionTokenRepository extends MongoRepository<VerificacionToken, String> {
    Optional<VerificacionToken> findByToken(String token);
    VerificacionToken findByEmail(String email);
    void deleteByEmail(String email);
    List<VerificacionToken> findByUsadoFalseAndExpiracionBefore(LocalDateTime fecha);
    List<VerificacionToken> findByUsadoTrue();
}
