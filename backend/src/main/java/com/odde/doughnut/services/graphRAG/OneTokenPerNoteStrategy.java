package com.odde.doughnut.services.graphRAG;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
public class OneTokenPerNoteStrategy implements TokenCountingStrategy {
  @Override
  public int estimateTokens(BareNote bareNote) {
    return 1;
  }

  @Override
  public String truncateByApproxTokens(String text, int maxTokens) {
    // Fake/no-op implementation: do not truncate
    return text;
  }
}
