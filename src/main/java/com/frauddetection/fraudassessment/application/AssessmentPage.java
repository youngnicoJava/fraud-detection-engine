package com.frauddetection.fraudassessment.application;

import java.util.List;

public record AssessmentPage<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {
  public AssessmentPage {
    items = List.copyOf(items);
  }
}
