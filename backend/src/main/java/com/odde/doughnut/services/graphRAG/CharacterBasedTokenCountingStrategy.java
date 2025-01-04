package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class CharacterBasedTokenCountingStrategy implements TokenCountingStrategy {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  @Override
  public int estimateTokens(BareNote bareNote) {
    String jsonString = objectMapper.writeValueAsString(bareNote);
    byte[] utf8Bytes = jsonString.getBytes("UTF-8");
    return (int) Math.ceil(utf8Bytes.length / 3.75);
  }
}
