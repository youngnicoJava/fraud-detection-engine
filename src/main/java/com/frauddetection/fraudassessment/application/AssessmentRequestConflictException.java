package com.frauddetection.fraudassessment.application;

public class AssessmentRequestConflictException extends RuntimeException {
  public AssessmentRequestConflictException() {
    super("Assessment request ID was reused with different material inputs");
  }
}
