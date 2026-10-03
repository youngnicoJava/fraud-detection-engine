package com.frauddetection.fraudassessment.api;

public final class CorrelationContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private CorrelationContext() {}

  public static void set(String value) {
    CURRENT.set(value);
  }

  public static String currentOr(String fallback) {
    String current = CURRENT.get();
    return current == null
        ? (fallback == null || fallback.isBlank()
            ? java.util.UUID.randomUUID().toString()
            : fallback)
        : current;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
