package com.reportes.urbanos.reportes_api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.reportes.urbanos.reportes_api.model.Usuario;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> 
{
   Optional <Usuario> findByEmail(String email);
}
