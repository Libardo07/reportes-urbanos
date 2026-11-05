package com.reportes.urbanos.reportes_api.controller; 

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee la predicción más reciente que guarda predecir.py en MongoDB
 * (colección predicciones_riesgo) y la combina con los barrios de MySQL.
 *
 *  GET /api/riesgo                -> barrios con sus predicciones por tipo de reporte
 *  GET /api/mapa-calor[?tipo=1]   -> puntos {lat, lng, peso 0..1} para el mapa de calor
 */
@RestController
@RequestMapping("/api")
public class RiesgoController {

    private static final String COL_PRED = "predicciones_riesgo";

    private final MongoTemplate mongo;   // MongoDB: predicciones
    private final JdbcTemplate jdbc;     // MySQL: barrios (id, nombre, lat, lng)

    public RiesgoController(MongoTemplate mongo, JdbcTemplate jdbc) {
        this.mongo = mongo;
        this.jdbc = jdbc;
    }

    private record Barrio(long id, String nombre, double lat, double lng) {}

    // ---------- lectura de datos ----------

    private List<Barrio> leerBarrios() {
        List<Barrio> barrios = new ArrayList<>();
        jdbc.query("SELECT id, nombre, lat, lng FROM barrios", rs -> {
            try {
                // getString tolera columnas DECIMAL, DOUBLE o VARCHAR
                double lat = Double.parseDouble(rs.getString("lat"));
                double lng = Double.parseDouble(rs.getString("lng"));
                barrios.add(new Barrio(rs.getLong("id"), rs.getString("nombre"), lat, lng));
            } catch (NullPointerException | NumberFormatException e) {
                // barrio sin coordenadas válidas: se omite
            }
        });
        return barrios;
    }

    /** Mes predicho más reciente, o null si no hay predicciones. */
    private String ultimoMes() {
        Document ultima = mongo.getCollection(COL_PRED).find()
                .sort(Sorts.descending("mesPredicho")).first();
        return ultima == null ? null : ultima.getString("mesPredicho");
    }

    private List<Document> leerPredicciones(String mes) {
        return mongo.getCollection(COL_PRED)
                .find(Filters.eq("mesPredicho", mes)).into(new ArrayList<>());
    }

    private static long entero(Document d, String campo) {
        return ((Number) d.get(campo)).longValue();
    }

    private static double decimal(Document d, String campo) {
        Object v = d.get(campo);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static ResponseEntity<Map<String, Object>> sinPredicciones() {
        return ResponseEntity.status(404).body(Map.of("error", "No hay predicciones en MongoDB."));
    }

    // ---------- endpoints ----------

    @GetMapping("/riesgo")
    public ResponseEntity<Map<String, Object>> riesgo() {
        String mes = ultimoMes();
        if (mes == null) return sinPredicciones();

        // barrioId -> tipoReporteId -> datos de la predicción
        Map<Long, Map<Long, Map<String, Object>>> porBarrio = new LinkedHashMap<>();
        for (Document p : leerPredicciones(mes)) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nivel", p.getString("nivel"));
            datos.put("nivelNumero", entero(p, "nivelNumero"));
            datos.put("probBajo", p.get("probBajo"));
            datos.put("probMedio", p.get("probMedio"));
            datos.put("probAlto", p.get("probAlto"));
            datos.put("reportes", p.get("reportesMesActual") == null ? 0 : p.get("reportesMesActual"));
            porBarrio.computeIfAbsent(entero(p, "barrioId"), k -> new LinkedHashMap<>())
                    .put(entero(p, "tipoReporteId"), datos);
        }

        List<Barrio> barrios = leerBarrios();
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Barrio b : barrios) {
            Map<Long, Map<String, Object>> tipos = porBarrio.get(b.id());
            if (tipos == null) continue;
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", b.id());
            fila.put("nombre", b.nombre());
            fila.put("lat", b.lat());
            fila.put("lng", b.lng());
            fila.put("tipos", tipos);
            salida.add(fila);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mes", mes);
        resp.put("barrios", salida);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/mapa-calor")
    public ResponseEntity<Map<String, Object>> mapaCalor(@RequestParam(required = false) Long tipo) {
        String mes = ultimoMes();
        if (mes == null) return sinPredicciones();

        // barrioId -> pesos (uno por tipo de reporte incluido)
        Map<Long, List<Double>> pesos = new LinkedHashMap<>();
        for (Document p : leerPredicciones(mes)) {
            if (tipo != null && entero(p, "tipoReporteId") != tipo) continue;
            // riesgo esperado: probabilidad Alta completa + Media a la mitad
            double w = decimal(p, "probMedio") * 0.5 + decimal(p, "probAlto");
            pesos.computeIfAbsent(entero(p, "barrioId"), k -> new ArrayList<>()).add(w);
        }

        List<Map<String, Object>> puntos = new ArrayList<>();
        for (Barrio b : leerBarrios()) {
            List<Double> ws = pesos.get(b.id());
            if (ws == null || ws.isEmpty()) continue;
            double promedio = ws.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("barrio", b.nombre());
            punto.put("lat", b.lat());
            punto.put("lng", b.lng());
            punto.put("peso", Math.round(promedio * 1000.0) / 1000.0);
            puntos.add(punto);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mes", mes);
        resp.put("tipo", tipo);
        resp.put("puntos", puntos);
        return ResponseEntity.ok(resp);
    }
}
