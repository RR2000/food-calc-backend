package com.rondinella.foodcalc.product;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenFoodFactsWarmUpTests {

  @Test
  void openFoodFactsNotReachableIsOnlyLogged() {
    // nothing listens on port 1: connection refused, twice (second try), and the warm-up just logs it
    OpenFoodFactsClient off = new OpenFoodFactsClient("http://127.0.0.1:1", "foodcalc/test", Duration.ofSeconds(1));
    assertThatCode(() -> new OpenFoodFactsWarmUp(off, "3017620422003").run()).doesNotThrowAnyException();
  }
}
