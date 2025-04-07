package com.rondinella.foodcalc.foodcalc.controller;

import java.net.http.HttpResponse;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.rondinella.foodcalc.foodcalc.dtos.ProductDTO;
import com.rondinella.foodcalc.foodcalc.services.MyService;

@RestController
public class MyController {

    @Autowired
    MyService myService;

    @CrossOrigin(origins = "http://localhost:8081")
    @GetMapping("product/{ean}")
    public ResponseEntity<ProductDTO> getMethodName(@PathVariable String ean) {
        return ResponseEntity.ok(myService.retriveProduct(ean));
    }

}
