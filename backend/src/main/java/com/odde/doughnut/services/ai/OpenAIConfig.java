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

    public OpenAIConfigBuilder setModel(String customModel) {
      if (customModel != null && !customModel.isEmpty()) {
        config.setModel(customModel);
      }
      return this;
    }

    public OpenAIConfigBuilder setTemperature(Double temp) {
      // Temperature is between 0 and 2, with default being 1
      // see: https://platform.openai.com/docs/api-reference/chat/create#temperature
      if (temp != null && temp >= 0 && temp <= 2.0) {
        config.setTemperature(temp);
      }
      return this;
    }
  }
}
