package com.reportes.urbanos.reportes_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/confirmacion")
public class ConfirmacionController {

    @GetMapping("/si/{reporteId}")
    public String confirmacionPositiva(@PathVariable String reporteId, Model model) {
        model.addAttribute("reporteId", reporteId);
        return "confirmacion_si";
    }

    @GetMapping("/no/{reporteId}")
    public String confirmacionNegativa(@PathVariable String reporteId, Model model) {
        model.addAttribute("reporteId", reporteId);
        return "confirmacion_no";
    }
}