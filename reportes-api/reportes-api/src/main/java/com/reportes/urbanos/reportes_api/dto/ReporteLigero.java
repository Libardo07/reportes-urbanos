package com.reportes.urbanos.reportes_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteLigero {
    private long estadoId;
    private long tipoId;
    private long barrioId;
    private long creado;   
}