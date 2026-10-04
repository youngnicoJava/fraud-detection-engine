package com.frauddetection.outbox.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KafkaOutboxAdapterTest {
  @Test
  void routesCaseEventsToTheResolutionTopic() {
    assertThat(KafkaOutboxAdapter.channelFor("fraud.case.resolved.v1"))
        .isEqualTo("fraud-case-resolutions");
    assertThat(KafkaOutboxAdapter.channelFor("fraud.assessment.completed.v1"))
        .isEqualTo("fraud-assessment-results");
  }
}
