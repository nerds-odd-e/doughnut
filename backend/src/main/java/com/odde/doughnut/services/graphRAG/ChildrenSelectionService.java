package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.*;
import java.util.stream.Collectors;

public class ChildrenSelectionService {
  private final Map<Note, List<Note>> childrenSorted = new HashMap<>();
  private final Map<Note, Set<Integer>> pickedChildIndices = new HashMap<>();

  public List<Note> selectChildren(
      Note parent,
      int remainingBudget,
      int depth,
      Map<Note, Integer> depthFetched) {
    List<Note> allChildren = getSortedChildren(parent);
    int childCap = calculateChildCap(parent, depth, depthFetched);
    int actualCap = Math.min(remainingBudget, childCap);

    if (actualCap <= 0) {
      return Collections.emptyList();
    }

    Set<Integer> picked = getPickedIndices(parent);
    List<Note> selected;

    if (picked.isEmpty()) {
      // Case A: First time - random contiguous block selection
      selected = selectRandomContiguousBlock(allChildren, actualCap);
    } else {
      // Case B: Already selected - distance-based selection (prefer nearby siblings)
      selected = selectByDistance(allChildren, picked, actualCap);
    }

    // Update picked indices
    for (Note child : selected) {
      int index = allChildren.indexOf(child);
      picked.add(index);
    }

    return selected;
  }

  private List<Note> getSortedChildren(Note parent) {
    return childrenSorted.computeIfAbsent(
        parent,
        p -> {
          List<Note> children = new ArrayList<>(p.getChildren());
          children.sort(Comparator.comparing(Note::getSiblingOrder));
          return children;
        });
  }

  private Set<Integer> getPickedIndices(Note parent) {
    return pickedChildIndices.computeIfAbsent(parent, p -> new HashSet<>());
  }

  private int calculateChildCap(Note parent, int depth, Map<Note, Integer> depthFetched) {
    Integer parentDepth = depthFetched.get(parent);
    if (parentDepth == null) {
      return GraphRAGConstants.CHILD_CAP_MULTIPLIER * depth;
    }
    return GraphRAGConstants.CHILD_CAP_MULTIPLIER * (depth - parentDepth);
  }

  private List<Note> selectRandomContiguousBlock(List<Note> children, int count) {
    if (children.isEmpty() || count <= 0) {
      return Collections.emptyList();
    }

    if (count >= children.size()) {
      return new ArrayList<>(children);
    }

    // For first selection, prefer earlier children (lower siblingOrder)
    // This matches the old behavior where children are processed in order
    return children.subList(0, count);
  }

  private List<Note> selectByDistance(
      List<Note> children, Set<Integer> pickedIndices, int count) {
    if (children.isEmpty() || count <= 0) {
      return Collections.emptyList();
    }

    if (pickedIndices.isEmpty()) {
      return selectRandomContiguousBlock(children, count);
    }

    // Find unpicked indices and their distances to nearest picked index
    List<IndexDistance> candidates = new ArrayList<>();
    for (int i = 0; i < children.size(); i++) {
      if (!pickedIndices.contains(i)) {
        int minDistance = Integer.MAX_VALUE;
        for (Integer pickedIndex : pickedIndices) {
          minDistance = Math.min(minDistance, Math.abs(i - pickedIndex));
        }
        candidates.add(new IndexDistance(i, minDistance));
      }
    }

    // Sort by distance (prefer nearby), then by index
    candidates.sort(
        Comparator.comparingInt((IndexDistance id) -> id.distance)
            .thenComparingInt(id -> id.index));

    // Select top candidates
    int selectCount = Math.min(count, candidates.size());
    return candidates.stream()
        .limit(selectCount)
        .map(c -> children.get(c.index))
        .collect(Collectors.toList());
  }

  private static class IndexDistance {
    final int index;
    final int distance;

    IndexDistance(int index, int distance) {
      this.index = index;
      this.distance = distance;
    }
  }
}
