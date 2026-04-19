package com.reportes.urbanos.reportes_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EmailValidationService {

    @Value("${abstractapi.email.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final String API_URL =
        "https://emailreputation.abstractapi.com/v1/?api_key={key}&email={email}";

    public EmailValidationService() {
        this.restTemplate = new RestTemplate();
    }

    public String validarEmail(String email) {

        log.info(">>> API KEY que está usando Spring: [{}]", apiKey);

        try {
            AbstractApiEmailResponse response = restTemplate.getForObject(
                API_URL,
                AbstractApiEmailResponse.class,
                apiKey, email
            );

            if (response == null) {
                log.warn("AbstractAPI no retornó respuesta para: {}", email);
                return null;
            }

            log.info("=== AbstractAPI Response ===");
            log.info("Email: {}", response.email());
            log.info("Deliverability: {}", response.deliverability());
            log.info("isDisposable: {}", response.isDisposableEmail());
            log.info("isMxFound: {}", response.isMxFound());
            log.info("===========================");

            if (response.isDisposableEmail() != null && response.isDisposableEmail().value()) {
                return "No se permiten correos temporales o desechables.";
            }

            if (response.isMxFound() != null && !response.isMxFound().value()) {
                return "El dominio del correo electrónico no existe.";
            }

            if ("UNDELIVERABLE".equals(response.deliverability())) {
                return "El correo electrónico no es válido o no existe.";
            }

            return null;

        } catch (Exception e) {
            log.warn("Error al contactar AbstractAPI, se omite validación: {}", e.getMessage());
            return null;
        }
    }
}