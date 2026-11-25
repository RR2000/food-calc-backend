package com.rondinella.foodcalc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class FoodcalcApplication {

  public static void main(String[] args) {
    SpringApplication.run(FoodcalcApplication.class, args);
  }

  /** Only for a web app served from another origin (the Expo dev server); in production nginx serves both. */
  @Bean
  WebMvcConfigurer cors(@Value("${foodcalc.cors-origins}") String[] origins) {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET");
      }
    };
  }
}
