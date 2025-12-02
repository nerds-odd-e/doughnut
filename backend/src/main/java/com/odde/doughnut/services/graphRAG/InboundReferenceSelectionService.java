package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.*;

public class InboundReferenceSelectionService {
  public List<Note> selectInboundReferences(
      Note target, int remainingBudget, int depth, Map<Note, Integer> depthFetched) {
    List<Note> allInbound = new ArrayList<>(target.getInboundReferences());
    if (allInbound.isEmpty()) {
      return Collections.emptyList();
    }

    int inboundCap = calculateInboundCap(target, depth, depthFetched);
    int actualCap = Math.min(remainingBudget, inboundCap);

    if (actualCap <= 0) {
      return Collections.emptyList();
    }

    // Random selection under per-depth caps
    Collections.shuffle(allInbound);
    return allInbound.subList(0, Math.min(actualCap, allInbound.size()));
  }

  private int calculateInboundCap(Note target, int depth, Map<Note, Integer> depthFetched) {
    Integer targetDepth = depthFetched.get(target);
    if (targetDepth == null) {
      return GraphRAGConstants.INBOUND_CAP_MULTIPLIER * depth;
    }
    return GraphRAGConstants.INBOUND_CAP_MULTIPLIER * (depth - targetDepth);
  }
}
