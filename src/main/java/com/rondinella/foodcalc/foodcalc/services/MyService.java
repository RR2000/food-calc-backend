package com.rondinella.foodcalc.foodcalc.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.rondinella.foodcalc.foodcalc.dtos.ProductDTO;

import pl.coderion.model.Product;
import pl.coderion.model.ProductResponse;
import pl.coderion.service.OpenFoodFactsWrapper;
import pl.coderion.service.impl.OpenFoodFactsWrapperImpl;

@Component
public class MyService {

    @Cacheable(value = "product", key = "#ean")
    public ProductDTO retriveProduct(String ean) {
        ProductDTO res = new ProductDTO();
        OpenFoodFactsWrapper wrapper = new OpenFoodFactsWrapperImpl();
        ProductResponse productResponse = wrapper.fetchProductByCode(ean);
        Product product = productResponse.getProduct();
        if (product != null) {
            res.setName(product.getProductName());
            res.setKcal(product.getNutriments().getEnergyKcal100G());
        }
        return res;
    }
}
