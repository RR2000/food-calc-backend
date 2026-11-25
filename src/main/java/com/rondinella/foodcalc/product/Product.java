package com.rondinella.foodcalc.product;

/**
 * A food product as the app needs it. Nutrients are per 100 g (or 100 ml); a value is null when
 * Open Food Facts does not know it.
 */
public record Product(
    String barcode,
    String name,
    String brand,
    String quantity,
    String imageUrl,
    Nutrients per100g) {

  public record Nutrients(Double kcal, Double protein, Double carbs, Double sugars, Double fat, Double salt) {}
}
