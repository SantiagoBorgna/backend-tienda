package com.tienda.web.controller;

import com.tienda.web.model.Articulo;
import com.tienda.web.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookOrdenController {

    @Autowired
    private ArticuloRepository articuloRepository;

    @PostMapping("/order")
    public ResponseEntity<Void> recibirOrdenTiendaNube(@RequestBody Map<String, Object> payload) {
        System.out.println("📦 Pedido recibido desde Tienda Nube:");
        System.out.println(payload);

        try {
            // Obtener productos del pedido
            List<Map<String, Object>> productos = (List<Map<String, Object>>) payload.get("products");

            for (Map<String, Object> producto : productos) {
                // El variant_id es el identificador que pusimos como idTiendaNube
                Long variantId = ((Number) producto.get("variant_id")).longValue();
                int cantidadVendida = ((Number) producto.get("quantity")).intValue();

                // Buscar el artículo en la base de datos por idTiendaNube
                Articulo articulo = articuloRepository.findAll().stream()
                        .filter(a -> a.getIdTiendaNube() != null && a.getIdTiendaNube().equals(variantId))
                        .findFirst()
                        .orElse(null);

                if (articulo == null) {
                    System.out.println("❗ No se encontró el artículo con variant_id: " + variantId);
                    continue;
                }

                int stockActual = articulo.getCant1();
                int nuevoStock = Math.max(0, stockActual - cantidadVendida);
                articulo.setCant1(nuevoStock);

                articuloRepository.save(articulo);
                System.out.println(
                        "✅ Stock actualizado de " + articulo.getNombre() + ": " + stockActual + " → " + nuevoStock);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("❌ Error al procesar el webhook de orden: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
