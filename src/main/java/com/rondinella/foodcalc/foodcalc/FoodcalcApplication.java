package com.rondinella.foodcalc.foodcalc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FoodcalcApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodcalcApplication.class, args);
	}

}
