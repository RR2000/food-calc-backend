package com.rondinella.foodcalc.product;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/** GET /api/products/{barcode}: name, brand and nutrients per 100 g of a product, from Open Food Facts. */
@RestController
public class ProductController {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);
  /** EAN-8, UPC-A, EAN-13, ITF-14 and the shorter in-store codes. */
  private static final Pattern BARCODE = Pattern.compile("\\d{6,14}");

  private final OpenFoodFactsClient off;
  /** Found and not found are both kept: Open Food Facts asks clients not to repeat the same reads. */
  private final Cache<String, Optional<Product>> cache;

  public ProductController(OpenFoodFactsClient off,
      @Value("${foodcalc.cache.size}") long size,
      @Value("${foodcalc.cache.ttl}") Duration ttl) {
    this.off = off;
    this.cache = Caffeine.newBuilder().maximumSize(size).expireAfterWrite(ttl).build();
  }

  @GetMapping("/api/products/{barcode}")
  public ResponseEntity<?> product(@PathVariable String barcode) {
    if (!BARCODE.matcher(barcode).matches()) {
      return error(HttpStatus.BAD_REQUEST, "a barcode is 6 to 14 digits");
    }
    Optional<Product> product = cache.get(barcode, off::find);
    return product.<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> error(HttpStatus.NOT_FOUND, "product not found"));
  }

  @ExceptionHandler(RestClientException.class)
  ResponseEntity<?> upstream(RestClientException e) {
    log.warn("Open Food Facts not reachable: {}", e.getMostSpecificCause().toString());
    return error(HttpStatus.BAD_GATEWAY, "Open Food Facts is not answering, try again later");
  }

  private static ResponseEntity<?> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("error", message));
  }
}
