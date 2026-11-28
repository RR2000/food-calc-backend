package com.rondinella.foodcalc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The API against a fake Open Food Facts on a local port: no network needed. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTests {

  static final List<String> calls = new CopyOnWriteArrayList<>();
  static final HttpServer off = fakeOpenFoodFacts();

  @Autowired
  MockMvc mvc;

  @DynamicPropertySource
  static void openFoodFacts(DynamicPropertyRegistry registry) {
    registry.add("foodcalc.off.base-url", () -> "http://127.0.0.1:" + off.getAddress().getPort());
    registry.add("foodcalc.off.timeout", () -> "2s");
    registry.add("foodcalc.off.warm-up-barcode", () -> "6000000000006");
  }

  @AfterAll
  static void stop() {
    off.stop(0);
  }

  @Test
  void productWithNutrientsPer100g() throws Exception {
    mvc.perform(get("/api/products/3017620422003"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.barcode").value("3017620422003"))
        .andExpect(jsonPath("$.name").value("Hazelnut spread"))
        .andExpect(jsonPath("$.brand").value("Brand A"))
        .andExpect(jsonPath("$.quantity").value("400 g"))
        .andExpect(jsonPath("$.imageUrl").value("https://images.example/front.jpg"))
        .andExpect(jsonPath("$.per100g.kcal").value(539.0))
        .andExpect(jsonPath("$.per100g.protein").value(6.3))
        .andExpect(jsonPath("$.per100g.carbs").value(57.5))
        .andExpect(jsonPath("$.per100g.fat").value(30.9))
        // a number sent as a string
        .andExpect(jsonPath("$.per100g.sugars").value(56.3))
        .andExpect(jsonPath("$.per100g.salt").value(nullValue()));
    assertThat(calls).anyMatch(c -> c.contains("food-calc-backend/1.0"));
  }

  @Test
  void kcalFromKilojoulesAndFallbackName() throws Exception {
    mvc.perform(get("/api/products/8000000000001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Water"))
        .andExpect(jsonPath("$.brand").value(nullValue()))
        .andExpect(jsonPath("$.imageUrl").value(nullValue()))
        .andExpect(jsonPath("$.per100g.kcal").value(100.0));
  }

  @Test
  void unknownBarcodeIs404AndIsAskedOnce() throws Exception {
    mvc.perform(get("/api/products/1234567890123"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("product not found"));
    mvc.perform(get("/api/products/1234567890123")).andExpect(status().isNotFound());
    assertThat(calls.stream().filter(c -> c.contains("1234567890123"))).hasSize(1);
  }

  @Test
  void notABarcodeIs400WithoutAskingOpenFoodFacts() throws Exception {
    mvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/products/123")).andExpect(status().isBadRequest());
    assertThat(calls).noneMatch(c -> c.contains("/abc") || c.contains("/123?"));
  }

  @Test
  void openFoodFactsDownIs502AndNotCached() throws Exception {
    mvc.perform(get("/api/products/5000000000005"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.error").value(containsString("Open Food Facts")));
    mvc.perform(get("/api/products/5000000000005")).andExpect(status().isBadGateway());
    assertThat(calls.stream().filter(c -> c.contains("5000000000005"))).hasSize(2);
  }

  @Test
  void aDroppedConnectionIsTriedAgain() throws Exception {
    mvc.perform(get("/api/products/7000000000007"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Second try"));
    assertThat(calls.stream().filter(c -> c.contains("7000000000007"))).hasSize(2);
  }

  @Test
  void openFoodFactsIsReadOnceRightAfterTheStart() throws Exception {
    // the warm-up runs in the background: give it a moment
    for (int i = 0; i < 50 && calls.stream().noneMatch(c -> c.contains("6000000000006")); i++) {
      Thread.sleep(100);
    }
    assertThat(calls.stream().filter(c -> c.contains("6000000000006"))).hasSize(1);
  }

  @Test
  void aProductWithNothingButTheBarcodeIsFoundWithEmptyValues() throws Exception {
    mvc.perform(get("/api/products/9000000000001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.barcode").value("9000000000001"))
        .andExpect(jsonPath("$.name").value(nullValue()))
        .andExpect(jsonPath("$.brand").value(nullValue()))
        .andExpect(jsonPath("$.quantity").value(nullValue()))
        .andExpect(jsonPath("$.per100g.kcal").value(nullValue()))
        .andExpect(jsonPath("$.per100g.protein").value(nullValue()));
  }

  @Test
  void oddBrandsAndValuesNeverBreakTheAnswer() throws Exception {
    // brands "," split to an empty array (it was a 500); numbers that are not numbers are unknown, not errors
    mvc.perform(get("/api/products/9000000000002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Odd"))
        .andExpect(jsonPath("$.brand").value(nullValue()))
        .andExpect(jsonPath("$.quantity").value(nullValue()))
        .andExpect(jsonPath("$.per100g.kcal").value(nullValue()))
        .andExpect(jsonPath("$.per100g.fat").value(nullValue()))
        .andExpect(jsonPath("$.per100g.salt").value(0.0));
    mvc.perform(get("/api/products/9000000000003"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brand").value("Second"));
  }

  @Test
  void foundWithoutAProductIsNotFound() throws Exception {
    mvc.perform(get("/api/products/9000000000004")).andExpect(status().isNotFound());
  }

  @Test
  void health() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  private static HttpServer fakeOpenFoodFacts() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/api/v2/product/", exchange -> {
        String path = exchange.getRequestURI().getPath();
        calls.add(exchange.getRequestURI() + " " + exchange.getRequestHeaders().getFirst("User-Agent"));
        String code = path.substring(path.lastIndexOf('/') + 1);
        if (code.equals("7000000000007") && calls.stream().filter(c -> c.contains(code)).count() == 1) {
          exchange.close(); // first try: connection closed without an answer
          return;
        }
        int status = 200;
        String body;
        switch (code) {
          case "3017620422003" -> body = """
              {"code":"3017620422003","status":1,"product":{"product_name":"Hazelnut spread","brands":"Brand A, Brand B",
               "quantity":"400 g e","image_front_small_url":"https://images.example/front.jpg",
               "nutriments":{"energy-kcal_100g":539,"proteins_100g":6.3,"carbohydrates_100g":57.5,
               "sugars_100g":"56.3","fat_100g":30.9}}}""";
          case "8000000000001" -> body = """
              {"code":"8000000000001","status":1,"product":{"product_name":"","generic_name":"Water",
               "image_front_small_url":"http://insecure.example/x.jpg","nutriments":{"energy_100g":418.4}}}""";
          case "7000000000007" -> body = """
              {"code":"7000000000007","status":1,"product":{"product_name":"Second try","nutriments":{}}}""";
          case "9000000000001" -> body = """
              {"code":"9000000000001","status":1,"product":{}}""";
          case "9000000000002" -> body = """
              {"code":"9000000000002","status":1,"product":{"product_name":"Odd","brands":",","quantity":" e",
               "nutriments":{"energy-kcal_100g":"n/a","energy_100g":"","fat_100g":null,"salt_100g":0}}}""";
          case "9000000000003" -> body = """
              {"code":"9000000000003","status":1,"product":{"product_name":"Brands","brands":" , Second",
               "nutriments":null}}""";
          case "9000000000004" -> body = """
              {"code":"9000000000004","status":1}""";
          case "5000000000005" -> {
            status = 503;
            body = "down";
          }
          default -> {
            status = 404;
            body = "{\"code\":\"" + code + "\",\"status\":0,\"status_verbose\":\"product not found\"}";
          }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
      });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
