package com.reportes.urbanos.reportes_api.repository;

import com.reportes.urbanos.reportes_api.entity.Barrio;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BarrioRepository extends MongoRepository<Barrio, String> {
}