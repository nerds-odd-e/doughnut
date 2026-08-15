package com.odde.doughnut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.testability.model.McqsTestData;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class TestabilityInjectMcqsPathTest {
  @Test
  void injectMcqsHttpIsCapabilityNamed() throws Exception {
    PostMapping mapping =
        TestabilityRestController.class
            .getDeclaredMethod("injectMcq", McqsTestData.class)
            .getAnnotation(PostMapping.class);
    assertThat(mapping.value()[0], equalTo("/inject-mcqs"));
  }
}
