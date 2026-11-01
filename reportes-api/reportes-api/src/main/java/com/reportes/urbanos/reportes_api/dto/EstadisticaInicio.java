package com.reportes.urbanos.reportes_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticaInicio {
    private Long estadoReporteId;
    private Long tipoReporteId;
    private long total;
    private LocalDateTime ultima;
}