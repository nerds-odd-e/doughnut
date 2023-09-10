package com.odde.doughnut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenAIConfig {
  public static final String DEFAULT_MODEL = "ft:gpt-3.5-turbo-0613:odd-e::7uWJuLEw";
  public static final Double DEFAULT_TEMPERATURE = 1.0;

  private String model;
  private Double temperature;

  private OpenAIConfig(String model, Double temperature) {
    this.model = model;
    this.temperature = temperature;
  }

  public static class OpenAIConfigBuilder {
    private final OpenAIConfig config = new OpenAIConfig(DEFAULT_MODEL, DEFAULT_TEMPERATURE);

    public OpenAIConfig build() {
      return config;
    }
  }
}
