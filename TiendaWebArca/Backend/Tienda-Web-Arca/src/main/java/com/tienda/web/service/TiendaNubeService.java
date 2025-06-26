package com.tienda.web.service;

import com.tienda.web.model.Articulo;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TiendaNubeService {

    private final String ACCESS_TOKEN = "794e9358306715511177c11653f3b47ed5a6a6f1";
    private final String API_URL = "https://api.tiendanube.com/v1/6374138/products";

    public void enviarProductoATiendaNube(Articulo articulo) {
        RestTemplate restTemplate = new RestTemplate();

        // Armar la lista de imágenes
        List<Map<String, String>> imagenes = new ArrayList<>();
        if (articulo.getImg1() != null && !articulo.getImg1().isBlank())
            imagenes.add(Map.of("src", articulo.getImg1()));
        if (articulo.getImg2() != null && !articulo.getImg2().isBlank())
            imagenes.add(Map.of("src", articulo.getImg2()));
        if (articulo.getImg3() != null && !articulo.getImg3().isBlank())
            imagenes.add(Map.of("src", articulo.getImg3()));
        if (articulo.getImg4() != null && !articulo.getImg4().isBlank())
            imagenes.add(Map.of("src", articulo.getImg4()));

        // Crear el cuerpo de la solicitud
        Map<String, Object> body = new HashMap<>();
        body.put("name", Map.of("es", articulo.getNombre()));
        body.put("description", Map.of("es", articulo.getDescripcion()));
        body.put("variants", List.of(
                Map.of(
                        "price", articulo.getPrecioVenta(),
                        "stock", articulo.getCant1() + articulo.getCant3())));
        if (!imagenes.isEmpty()) {
            body.put("images", imagenes);
        }

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        System.out.println("Enviando producto: " + body);
        System.out.println("Request completo: " + request);

        // Enviar POST
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
            System.out.println("Enviado a Tienda Nube: " + articulo.getNombre());
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Producto creado correctamente en Tienda Nube.");
            } else {
                System.out.println("Algo no anduvo bien, aunque no explotó.");
            }
        } catch (Exception e) {
            System.err.println("Error al enviar el artículo " + articulo.getNombre() + ": " + e.getMessage());
        }
    }

    public void getProductosDesdeTiendaNube() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.GET,
                    request,
                    String.class);
            System.out.println("Respuesta desde Tienda Nube (GET productos):");
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
        } catch (Exception e) {
            System.err.println("Error al hacer GET de productos: " + e.getMessage());
        }
    }
}
