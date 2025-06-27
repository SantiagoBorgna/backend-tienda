package com.tienda.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.web.model.Articulo;
import com.tienda.web.repository.ArticuloRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TiendaNubeService {

    @Autowired
    private ArticuloService articuloService;
    @Autowired
    private ArticuloRepository articuloRepository;

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
                        "promotional_price", articulo.getPrecioVenta() * 0.8,
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
                // Extraer ID del producto creado
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseData = mapper.readValue(response.getBody(), Map.class);
                Long idTiendaNube = ((Number) responseData.get("id")).longValue();

                // Guardar el ID en la base de datos
                articulo.setIdTiendaNube(idTiendaNube);
                articuloRepository.save(articulo);

                System.out.println("Producto creado correctamente. ID Tienda Nube: " + idTiendaNube);
            } else {
                System.out.println("Algo no anduvo bien, aunque no explotó.");
            }
        } catch (Exception e) {
            System.err.println("Error al enviar el artículo " + articulo.getNombre() + ": " + e.getMessage());
        }
    }

    public List<Map<String, Object>> obtenerProductosTiendaNube() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                API_URL,
                HttpMethod.GET,
                request,
                List.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return Collections.emptyList();
        }
    }

    public void sincronizarArticulos(List<Articulo> articulosLocales) {
        List<Map<String, Object>> productosEnTienda = obtenerProductosTiendaNube();

        for (Articulo articulo : articulosLocales) {
            Map<String, Object> productoExistente = productosEnTienda.stream()
                    .filter(p -> {
                        Map<String, String> nameMap = (Map<String, String>) p.get("name");
                        return nameMap != null && nameMap.get("es").equalsIgnoreCase(articulo.getNombre());
                    })
                    .findFirst()
                    .orElse(null);

            if (productoExistente == null) {
                enviarProductoATiendaNube(articulo);
            } else {
                Long idProductoTienda = ((Number) productoExistente.get("id")).longValue();
                actualizarProductoEnTiendaNube(idProductoTienda, articulo);
            }
        }
    }

    public void actualizarProductoEnTiendaNube(Long idTiendaNube, Articulo articulo) {
        RestTemplate restTemplate = new RestTemplate();

        List<Map<String, String>> imagenes = new ArrayList<>();
        if (articulo.getImg1() != null && !articulo.getImg1().isBlank())
            imagenes.add(Map.of("src", articulo.getImg1()));
        if (articulo.getImg2() != null && !articulo.getImg2().isBlank())
            imagenes.add(Map.of("src", articulo.getImg2()));
        if (articulo.getImg3() != null && !articulo.getImg3().isBlank())
            imagenes.add(Map.of("src", articulo.getImg3()));
        if (articulo.getImg4() != null && !articulo.getImg4().isBlank())
            imagenes.add(Map.of("src", articulo.getImg4()));

        Map<String, Object> body = new HashMap<>();
        body.put("name", Map.of("es", articulo.getNombre()));
        body.put("description", Map.of("es", articulo.getDescripcion()));
        body.put("variants", List.of(
                Map.of(
                        "price", articulo.getPrecioVenta(),
                        "promotional_price", articulo.getPrecioVenta() * 0.8,
                        "stock", articulo.getCant1() + articulo.getCant3())));
        if (!imagenes.isEmpty()) {
            body.put("images", imagenes);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String urlUpdate = API_URL + "/" + idTiendaNube;

        try {
            restTemplate.exchange(urlUpdate, HttpMethod.PUT, request, String.class);
            System.out.println("Producto actualizado en Tienda Nube: " + articulo.getNombre());
        } catch (Exception e) {
            System.err.println("Error al actualizar el artículo " + articulo.getNombre() + ": " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // Todos los días a las 03:00 AM
    public void sincronizacionAutomatica() {
        System.out.println(">>> Iniciando sincronización automática con Tienda Nube...");

        // Este método deberías tenerlo en ArticuloService
        List<Articulo> articulos = articuloService.obtenerArticulosDeArca();

        sincronizarArticulos(articulos);

        System.out.println(">>> Sincronización automática finalizada.");
    }

}
