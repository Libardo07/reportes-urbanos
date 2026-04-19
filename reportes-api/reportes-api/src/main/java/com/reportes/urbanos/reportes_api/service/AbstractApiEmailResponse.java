package com.reportes.urbanos.reportes_api.service;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbstractApiEmailResponse(
    String email,
    String deliverability,
    @JsonProperty("is_disposable_email") QualityScore isDisposableEmail,
    @JsonProperty("is_mx_found")         QualityScore isMxFound
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QualityScore(boolean value) {}
}