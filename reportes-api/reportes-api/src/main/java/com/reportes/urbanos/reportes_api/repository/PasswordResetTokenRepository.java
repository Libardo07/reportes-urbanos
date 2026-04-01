package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    PasswordResetToken findByToken(String token);
    void deleteByEmail(String email);
}