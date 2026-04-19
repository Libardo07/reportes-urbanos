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
        "https://emailvalidation.abstractapi.com/v1/?api_key={key}&email={email}";

    public EmailValidationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * @return null si el email es válido, o un mensaje de error si no lo es.
     */
    public String validarEmail(String email) {
        try {
            AbstractApiEmailResponse response = restTemplate.getForObject(
                API_URL,
                AbstractApiEmailResponse.class,
                apiKey, email
            );

            if (response == null) {
                log.warn("AbstractAPI no retornó respuesta para: {}", email);
                return null; // Si la API falla, dejamos pasar
            }

            if (response.isDisposableEmail() != null && response.isDisposableEmail().value()) {
                return "No se permiten correos temporales o desechables.";
            }

            if (response.isMxFound() != null && !response.isMxFound().value()) {
                return "El dominio del correo electrónico no existe.";
            }

            if ("UNDELIVERABLE".equals(response.deliverability())) {
                return "El correo electrónico no es válido o no existe.";
            }

            log.info("Email validado OK: {} → {}", email, response.deliverability());
            return null; // Todo bien ✅

        } catch (Exception e) {
            log.warn("Error al contactar AbstractAPI, se omite validación: {}", e.getMessage());
            return null; // Si la API está caída, no bloqueamos el registro
        }
    }
}
