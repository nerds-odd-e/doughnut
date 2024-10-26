package com.odde.doughnut.configs;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.handlers.AudioWebSocketHandler;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Autowired private ModelFactoryService modelFactoryService;

  @Autowired
  @Qualifier("testableOpenAiApi")
  private OpenAiApi openAiApi;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(audioWebSocketHandler(), "/ws/audio").setAllowedOrigins("*");
  }

  @Bean
  public WebSocketHandler audioWebSocketHandler() {
    return new AudioWebSocketHandler(openAiApi, modelFactoryService);
  }
}
