package com.odde.doughnut.services.ai;

import static com.odde.doughnut.services.ai.OpenAIConfig.DEFAULT_MODEL;
import static com.odde.doughnut.services.ai.OpenAIConfig.DEFAULT_TEMPERATURE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class OpenAIConfigTest {

  @Test
  void createDefaultOpenAIConfig() {
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.build();
    assertThat(config.getModel(), equalTo(DEFAULT_MODEL));
    assertThat(config.getTemperature(), equalTo(DEFAULT_TEMPERATURE));
  }

  @Test
  void setCustomModelInOpenAIConfig() {
    String customModel = "my-model";
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setModel(customModel).build();
    assertThat(config.getModel(), equalTo(customModel));
  }

  @Test
  void settingEmptyModelWillUseDefaultModel() {
    String emptyStringModel = "";
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setModel(emptyStringModel).build();
    assertThat(config.getModel(), equalTo(DEFAULT_MODEL));
  }

  @Test
  void settingNullModelWillUseDefaultModel() {
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setModel(null).build();
    assertThat(config.getModel(), equalTo(DEFAULT_MODEL));
  }

  @Test
  void setTemperatureInOpenAIConfig() {
    Double temp = 0.5;
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setTemperature(temp).build();
    assertThat(config.getTemperature(), equalTo(temp));
  }

  @Test
  void setNegativeTemperatureInOpenAIConfigWillUseDefault() {
    Double temp = -0.5;
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setTemperature(temp).build();
    assertThat(config.getTemperature(), equalTo(DEFAULT_TEMPERATURE));
  }

  @Test
  void setTemperatureAboveTwoInOpenAIConfigWillUseDefault() {
    Double temp = 2.1;
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setTemperature(temp).build();
    assertThat(config.getTemperature(), equalTo(DEFAULT_TEMPERATURE));
  }

  @Test
  void setNullTemperatureTwoInOpenAIConfigWillUseDefault() {
    OpenAIConfig.OpenAIConfigBuilder builder = new OpenAIConfig.OpenAIConfigBuilder();
    OpenAIConfig config = builder.setTemperature(null).build();
    assertThat(config.getTemperature(), equalTo(DEFAULT_TEMPERATURE));
  }
}
