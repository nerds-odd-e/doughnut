package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class InboundReferenceSelectionService {
  private static final int INBOUND_CAP_MULTIPLIER = 2;

  /**
   * Selects inbound references for a target note at a given depth, applying per-depth caps and
   * random selection.
   *
   * @param target The note for which to select inbound references
   * @param currentDepth The current depth in the traversal
   * @param depthFetched Map tracking the depth at which each note was first discovered
   * @param alreadyEmitted Number of inbound references already emitted for this target
   * @param allInboundReferences All available inbound references (should already be shuffled for
   *     randomness)
   * @return List of selected inbound references (up to the remaining budget)
   */
  public List<Note> selectInboundReferences(
      Note target,
      int currentDepth,
      java.util.Map<Note, Integer> depthFetched,
      int alreadyEmitted,
      List<Note> allInboundReferences) {
    int inboundCap = calculateInboundCap(target, currentDepth, depthFetched);
    int remainingBudget = Math.max(0, inboundCap - alreadyEmitted);
    return allInboundReferences.subList(0, Math.min(remainingBudget, allInboundReferences.size()));
  }

  private int calculateInboundCap(
      Note target, int currentDepth, java.util.Map<Note, Integer> depthFetched) {
    int targetDepthFetched = depthFetched.getOrDefault(target, currentDepth);
    return INBOUND_CAP_MULTIPLIER * (currentDepth - targetDepthFetched);
  }
}
