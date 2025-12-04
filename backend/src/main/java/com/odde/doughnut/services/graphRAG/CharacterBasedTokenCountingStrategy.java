package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class CharacterBasedTokenCountingStrategy implements TokenCountingStrategy {
  private static final double BYTES_PER_TOKEN = 3.75d;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @SneakyThrows
  @Override
  public int estimateTokens(BareNote bareNote) {
    String jsonString = objectMapper.writeValueAsString(bareNote);
    byte[] utf8Bytes = jsonString.getBytes(StandardCharsets.UTF_8);
    return (int) Math.ceil(utf8Bytes.length / BYTES_PER_TOKEN);
  }

  /**
   * Truncate a string so its approximate token count (using ~3.75 UTF-8 bytes per token) does not
   * exceed maxTokens.
   */
  @Override
  public String truncateByApproxTokens(String text, int maxTokens) {
    if (text == null || text.isEmpty()) return text;
    int maxBytes = (int) Math.floor(maxTokens * BYTES_PER_TOKEN);
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= maxBytes) return text;

    int low = 0;
    int high = text.length();
    while (low < high) {
      int mid = (low + high + 1) / 2;
      int len = text.substring(0, mid).getBytes(StandardCharsets.UTF_8).length;
      if (len <= maxBytes) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    return text.substring(0, low);
  }
}
