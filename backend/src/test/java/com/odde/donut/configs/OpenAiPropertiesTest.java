package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

class OpenAiPropertiesTest extends SpringTestBase {
  @Autowired OpenAiProperties openAiProperties;
  @Autowired Environment environment;

  @Test
  void bindsTheConfiguredOpenAiToken() {
    assertThat(openAiProperties.token(), equalTo(environment.getProperty("spring.openai.token")));
  }
}
