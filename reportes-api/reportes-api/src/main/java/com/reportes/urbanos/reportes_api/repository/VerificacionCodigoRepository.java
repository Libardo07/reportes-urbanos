package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.VerificacionCodigo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VerificacionCodigoRepository extends MongoRepository<VerificacionCodigo, String> {
    VerificacionCodigo findByEmail(String email);
    void deleteByEmail(String email);
}
