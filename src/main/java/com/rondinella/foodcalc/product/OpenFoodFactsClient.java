package com.rondinella.foodcalc.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads a product from the Open Food Facts API (v2). Open Food Facts asks every client to send a User-Agent
 * that names the application.
 */
@Component
public class OpenFoodFactsClient {

  private static final String FIELDS =
      "code,product_name,product_name_en,generic_name,brands,quantity,image_front_small_url,nutriments";

  private final RestClient http;

  public OpenFoodFactsClient(
      @Value("${foodcalc.off.base-url}") String baseUrl,
      @Value("${foodcalc.off.user-agent}") String userAgent,
      @Value("${foodcalc.off.timeout}") Duration timeout) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(timeout);
    this.http = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .defaultHeader("User-Agent", userAgent)
        .build();
  }

  /** Empty when Open Food Facts does not have the barcode; an exception when it cannot be reached. */
  public Optional<Product> find(String barcode) {
    OffResponse response = http.get()
        .uri("/api/v2/product/{code}?fields={fields}", barcode, FIELDS)
        .retrieve()
        // "not found" comes back as 404 with a JSON body (status 0): it is an answer, not an error
        .onStatus(s -> s.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {})
        .body(OffResponse.class);
    if (response == null || response.product() == null || !Integer.valueOf(1).equals(response.status())) {
      return Optional.empty();
    }
    return Optional.of(response.product().toProduct(barcode));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OffResponse(Integer status, OffProduct product) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record OffProduct(
      @JsonProperty("product_name") String productName,
      @JsonProperty("product_name_en") String productNameEn,
      @JsonProperty("generic_name") String genericName,
      String brands,
      String quantity,
      @JsonProperty("image_front_small_url") String imageUrl,
      Map<String, Object> nutriments) {

    Product toProduct(String barcode) {
      Map<String, Object> n = nutriments == null ? Map.of() : nutriments;
      Double kcal = number(n.get("energy-kcal_100g"));
      if (kcal == null) {
        Double kj = number(n.get("energy_100g"));
        kcal = kj == null ? null : kj / 4.184;
      }
      return new Product(
          barcode,
          firstNonBlank(productName, productNameEn, genericName),
          brands == null ? null : firstNonBlank(brands.split(",")[0]),
          firstNonBlank(quantity),
          imageUrl != null && imageUrl.startsWith("https://") ? imageUrl : null,
          new Product.Nutrients(
              round(kcal),
              round(number(n.get("proteins_100g"))),
              round(number(n.get("carbohydrates_100g"))),
              round(number(n.get("sugars_100g"))),
              round(number(n.get("fat_100g"))),
              round(number(n.get("salt_100g")))));
    }
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v.strip();
      }
    }
    return null;
  }

  /** Open Food Facts sends numbers either as JSON numbers or as strings. */
  static Double number(Object value) {
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    if (value instanceof String s && !s.isBlank()) {
      try {
        return Double.valueOf(s.strip().replace(',', '.'));
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  private static Double round(Double value) {
    if (value == null || value.isNaN() || value < 0) {
      return null;
    }
    return Math.round(value * 10) / 10.0;
  }
}
