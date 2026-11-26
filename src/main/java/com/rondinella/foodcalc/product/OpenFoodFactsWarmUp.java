package com.rondinella.foodcalc.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * One read from Open Food Facts right after the start, in the background: the first request of a fresh JVM pays
 * for DNS, the TLS handshake and the cold classes, and on prod it hit the timeout (about 9 s with the second try).
 * Paid here, the first real search finds a warm client and an open connection. A failure is only logged.
 */
@Component
public class OpenFoodFactsWarmUp {

  private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsWarmUp.class);

  private final OpenFoodFactsClient off;
  private final String barcode;

  public OpenFoodFactsWarmUp(OpenFoodFactsClient off, @Value("${foodcalc.off.warm-up-barcode:}") String barcode) {
    this.off = off;
    this.barcode = barcode == null ? "" : barcode.strip();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (barcode.isEmpty()) {
      return;
    }
    Thread.ofVirtual().name("off-warm-up").start(this::run);
  }

  void run() {
    long t0 = System.nanoTime();
    try {
      boolean found = off.find(barcode).isPresent();
      log.info("Open Food Facts warm-up: answered in {} ms (product {})", (System.nanoTime() - t0) / 1_000_000,
          found ? "found" : "not found");
    } catch (RuntimeException e) {
      Throwable cause = e;
      while (cause.getCause() != null && cause.getCause() != cause) {
        cause = cause.getCause();
      }
      log.warn("Open Food Facts warm-up failed after {} ms: {}", (System.nanoTime() - t0) / 1_000_000, cause.toString());
    }
  }
}
