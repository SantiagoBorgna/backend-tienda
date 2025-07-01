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
        body.put("custom_product_type", articulo.getCategoria());
        body.put("variants", List.of(
                Map.of(
                        "price", articulo.getPrecioVenta(),
                        "promotional_price", articulo.getPrecioVenta() * 0.8,
                        "stock", articulo.getCant1())));
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");

        try {
            // Paso 1: actualizar nombre y descripción
            Map<String, Object> bodyProducto = new HashMap<>();
            bodyProducto.put("name", Map.of("es", articulo.getNombre()));
            bodyProducto.put("description", Map.of("es", articulo.getDescripcion()));
            bodyProducto.put("custom_product_type", articulo.getCategoria());

            HttpEntity<Map<String, Object>> requestProducto = new HttpEntity<>(bodyProducto, headers);
            String urlProducto = API_URL + "/" + idTiendaNube;

            restTemplate.exchange(urlProducto, HttpMethod.PUT, requestProducto, String.class);

            // Paso 2: obtener el producto para extraer el variant_id
            HttpEntity<Void> requestGet = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(urlProducto, HttpMethod.GET, requestGet, Map.class);

            List<Map<String, Object>> variants = (List<Map<String, Object>>) response.getBody().get("variants");
            if (variants == null || variants.isEmpty()) {
                System.err.println("No se encontraron variantes para el producto: " + articulo.getNombre());
                return;
            }

            Long variantId = ((Number) variants.get(0).get("id")).longValue();

            // Paso 3: actualizar precio, stock y descuento del variant
            Map<String, Object> bodyVariant = new HashMap<>();
            bodyVariant.put("price", articulo.getPrecioVenta());
            bodyVariant.put("promotional_price", articulo.getPrecioVenta() * 0.8);
            bodyVariant.put("stock", articulo.getCant1());

            HttpEntity<Map<String, Object>> requestVariant = new HttpEntity<>(bodyVariant, headers);
            String urlVariant = urlProducto + "/variants/" + variantId;

            restTemplate.exchange(urlVariant, HttpMethod.PUT, requestVariant, String.class);

            System.out.println("Producto actualizado en Tienda Nube: " + articulo.getNombre());

        } catch (Exception e) {
            System.err.println("Error al actualizar el artículo " + articulo.getNombre() + ": " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 */4 * * *") // Cada 4hs
    public void sincronizacionAutomatica() {
        System.out.println(">>> Iniciando sincronización automática con Tienda Nube...");

        // Este método deberías tenerlo en ArticuloService
        List<Articulo> articulos = articuloService.obtenerArticulosDeArca();

        sincronizarArticulos(articulos);

        System.out.println(">>> Sincronización automática finalizada.");
    }

    public void registrarWebhookOrdenCreada() {
        RestTemplate restTemplate = new RestTemplate();

        String url = "https://api.tiendanube.com/v1/6374138/webhooks";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authentication", "bearer " + ACCESS_TOKEN);
        headers.set("User-Agent", "Integrador El Arca Home (santiborgna5@gmail.com)");

        Map<String, Object> body = new HashMap<>();
        body.put("event", "order/created");
        body.put("url", "https://backend-tienda-9gtc.onrender.com/api/webhook/order");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Webhook creado: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error al registrar el webhook: " + e.getMessage());
        }
    }

}
