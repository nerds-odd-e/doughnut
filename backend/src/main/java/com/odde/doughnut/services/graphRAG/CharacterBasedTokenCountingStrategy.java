package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;

public class CharacterBasedTokenCountingStrategy implements TokenCountingStrategy {
  private final ObjectMapper objectMapper =
      new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();

  @SneakyThrows
  @Override
  public int estimateTokens(BareNote bareNote) {
    String jsonString = objectMapper.writeValueAsString(bareNote);
    byte[] utf8Bytes = jsonString.getBytes(StandardCharsets.UTF_8);
    return (int) Math.ceil(utf8Bytes.length / 3.75);
  }
}
