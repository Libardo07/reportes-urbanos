package com.reportes.urbanos.reportes_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    // Sube la imagen a AWS S3 y devuelve la URL pública del archivo
    public String subirImagen(MultipartFile archivo) {
        try {
            System.out.println("=== Subiendo imagen a S3 ===");
            System.out.println("Nombre: " + archivo.getOriginalFilename());
            System.out.println("Tamaño: " + archivo.getSize());
            System.out.println("Tipo: " + archivo.getContentType());
            
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String key = "reportes/" + UUID.randomUUID() + "." + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(archivo.getContentType())
                .build();

            s3Client.putObject(request, RequestBody.fromBytes(archivo.getBytes()));

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
            System.out.println("=== URL generada: " + url);
            return url;

        } catch (IOException e) {
            System.err.println("=== ERROR S3: " + e.getMessage());
            throw new RuntimeException("Error al subir imagen a S3: " + e.getMessage());
        }
}

    // Sube un máximo de 3 imágenes y devuelve sus URLs públicas
    public List<String> subirImagenes(List<MultipartFile> archivos) {
        List<String> urls = new ArrayList<>();
        if (archivos == null || archivos.isEmpty()) return urls;

        for (MultipartFile archivo : archivos) {
            if (archivo != null && !archivo.isEmpty()) {
                urls.add(subirImagen(archivo));
                if (urls.size() >= 3) break;
            }
        }
        return urls;
    }

    // Elimina la imagen almacenada en AWS S3 usando su URL
    public void eliminarImagen(String url) {
        if (url == null || url.isBlank()) return;
        try {
            String key = url.substring(url.indexOf("reportes/"));
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        } catch (Exception e) {
            System.err.println("Error al eliminar imagen S3: " + e.getMessage());
        }
    }

    // Elimina varias imágenes de AWS S3 usando sus URLs
    public void eliminarImagenes(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        urls.forEach(this::eliminarImagen);
    }

    // Obtiene la extensión de un archivo o devuelve jpg por defecto
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) return "jpg";
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }
}
