package com.reportes.urbanos.reportes_api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.reportes.urbanos.reportes_api.model.Barrio;

@Repository
public interface BarrioRepository extends MongoRepository<Barrio, String> {}