package com.reportes.urbanos.reportes_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.reportes.urbanos.reportes_api.repository",
    includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.reportes.urbanos.reportes_api.repository.ReporteRepository.class,
            com.reportes.urbanos.reportes_api.repository.UsuarioRepository.class,
            com.reportes.urbanos.reportes_api.repository.ComentarioRepository.class,
            com.reportes.urbanos.reportes_api.repository.VerificacionTokenRepository.class,
            com.reportes.urbanos.reportes_api.repository.PasswordResetTokenRepository.class
        }
    ))
@EnableJpaRepositories(basePackages = "com.reportes.urbanos.reportes_api.repository",
    includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.reportes.urbanos.reportes_api.repository.BarrioRepository.class,
            com.reportes.urbanos.reportes_api.repository.TipoReporteRepository.class,
            com.reportes.urbanos.reportes_api.repository.EstadoReporteRepository.class
        }
    ))
public class DatabaseConfig {}