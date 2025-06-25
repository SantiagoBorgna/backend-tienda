package com.tienda.web.service;

import com.tienda.web.model.Articulo;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TiendaNubeService {

    private final String ACCESS_TOKEN = "0e587efcbd2804630fec53042f38cce6b4877b4d";
    private final String API_URL = "https://api.tiendanube.com/2025-03/6374138/products";

    public void enviarProductoATiendaNube(Articulo articulo) {
        RestTemplate restTemplate = new RestTemplate();

        // Armar la lista de imágenes solo con las que no están vacías
        List<Map<String, String>> imagenes = new ArrayList<>();

        if (articulo.getImg1() != null && !articulo.getImg1().isBlank()) {
            imagenes.add(Map.of("src", articulo.getImg1()));
        }
        if (articulo.getImg2() != null && !articulo.getImg2().isBlank()) {
            imagenes.add(Map.of("src", articulo.getImg2()));
        }
        if (articulo.getImg3() != null && !articulo.getImg3().isBlank()) {
            imagenes.add(Map.of("src", articulo.getImg3()));
        }
        if (articulo.getImg4() != null && !articulo.getImg4().isBlank()) {
            imagenes.add(Map.of("src", articulo.getImg4()));
        }

        // Crear el cuerpo de la solicitud
        Map<String, Object> body = new HashMap<>();
        body.put("name", Map.of("es", articulo.getNombre()));
        body.put("description", Map.of("es", articulo.getDescripcion()));
        body.put("price", articulo.getPrecioVenta());
        body.put("stock", articulo.getCant1() + articulo.getCant3());
        if (!imagenes.isEmpty()) {
            body.put("images", imagenes);
        }

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ACCESS_TOKEN);
        headers.set("User-Agent", "ElArcaHome (santiborgna5@gmail.com)");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Enviar POST
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
            System.out.println("Enviado a Tienda Nube: " + articulo.getNombre());
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
        } catch (Exception e) {
            System.err.println("Error al enviar el artículo " + articulo.getNombre() + ": " + e.getMessage());
        }
    }

}
