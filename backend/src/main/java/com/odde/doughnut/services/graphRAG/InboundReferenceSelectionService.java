package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.*;

public class InboundReferenceSelectionService {
  private final Random random;

  public InboundReferenceSelectionService() {
    this.random = new Random();
  }

  public InboundReferenceSelectionService(Random random) {
    this.random = random;
  }

  public List<Note> selectInboundReferences(
      Note target, int remainingBudget, int depth, Map<Note, Integer> depthFetched) {
    if (remainingBudget <= 0) {
      return Collections.emptyList();
    }

    List<Note> inboundRefs = new ArrayList<>(target.getInboundReferences());
    if (inboundRefs.isEmpty()) {
      return Collections.emptyList();
    }

    Collections.shuffle(inboundRefs, random);

    int count = Math.min(remainingBudget, inboundRefs.size());
    return inboundRefs.subList(0, count);
  }
}

