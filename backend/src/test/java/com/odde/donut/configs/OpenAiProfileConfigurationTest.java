package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class OpenAiProfileConfigurationTest {
  @ParameterizedTest
  @CsvSource({
    "e2e, , donut-e2e-placeholder",
    "e2e, synthetic-supplied-token, synthetic-supplied-token",
    "dev, , ''"
  })
  void resolvesProfileCredential(String profile, String suppliedToken, String expectedToken) {
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    Map<String, Object> inputs = new HashMap<>();
    inputs.put(
        "spring.config.location",
        Path.of("src/main/resources/application.yml").toAbsolutePath().toUri().toString());
    inputs.put("spring.profiles.active", profile);
    if (suppliedToken != null) {
      inputs.put("OPENAI_API_TOKEN", suppliedToken);
    }
    environment.getPropertySources().addFirst(new MapPropertySource("launchInputs", inputs));

    ConfigDataEnvironmentPostProcessor.applyTo(environment);

    assertThat(environment.getProperty("spring.openai.token"), equalTo(expectedToken));
  }
}
