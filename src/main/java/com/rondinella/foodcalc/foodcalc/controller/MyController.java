package com.rondinella.foodcalc.foodcalc.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.coderion.model.ProductResponse;
import pl.coderion.service.OpenFoodFactsWrapper;
import pl.coderion.service.impl.OpenFoodFactsWrapperImpl;

@RestController
public class MyController {

    @CrossOrigin(origins = "http://localhost:8081")
    @GetMapping("path")
    public Map<String, Object> getMethodName(@RequestParam("EAN") String ean) {
        Map<String, Object> res = new HashMap<>();
        OpenFoodFactsWrapper wrapper = new OpenFoodFactsWrapperImpl();
        ProductResponse productResponse = wrapper.fetchProductByCode(ean);
        res.put("name", productResponse.getProduct().getProductName());
        res.put("kcal", productResponse.getProduct().getNutriments().getEnergyKcal100G());
        return res;
    }

}
