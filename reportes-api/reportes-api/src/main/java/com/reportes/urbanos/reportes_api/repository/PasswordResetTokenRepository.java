package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    PasswordResetToken findByToken(String token);
    void deleteByEmail(String email);
    List<PasswordResetToken> findByExpiracionBefore(LocalDateTime fecha);
}