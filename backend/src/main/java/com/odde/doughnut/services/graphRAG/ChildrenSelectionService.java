package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChildrenSelectionService {
  private final Map<Note, List<Note>> childrenSorted = new HashMap<>();
  private final Map<Note, Set<Integer>> pickedChildIndices = new HashMap<>();
  private final Random random;

  public ChildrenSelectionService() {
    this.random = new Random();
  }

  public ChildrenSelectionService(Random random) {
    this.random = random;
  }

  public List<Note> selectChildren(
      Note parent, int remainingBudget, int depth, Map<Note, Integer> depthFetched) {
    if (remainingBudget <= 0) {
      return Collections.emptyList();
    }

    List<Note> children = getSortedChildren(parent);
    if (children.isEmpty()) {
      return Collections.emptyList();
    }

    Set<Integer> picked = pickedChildIndices.getOrDefault(parent, new HashSet<>());

    if (picked.isEmpty()) {
      return selectRandomContiguousBlock(children, remainingBudget);
    } else {
      return selectNearbySiblings(children, picked, remainingBudget);
    }
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

  private List<Note> selectRandomContiguousBlock(List<Note> children, int k) {
    int n = children.size();
    if (k >= n) {
      return new ArrayList<>(children);
    }

    int maxStart = n - k;
    int start = random.nextInt(maxStart + 1);
    List<Note> selected = new ArrayList<>();
    for (int i = start; i < start + k && i < n; i++) {
      selected.add(children.get(i));
    }

    Note parent = selected.get(0).getParent();
    Set<Integer> picked = pickedChildIndices.computeIfAbsent(parent, p -> new HashSet<>());
    for (int i = start; i < start + k && i < n; i++) {
      picked.add(i);
    }

    return selected;
  }

  private List<Note> selectNearbySiblings(
      List<Note> children, Set<Integer> pickedIndices, int remainingBudget) {
    int n = children.size();
    List<CandidateWithDistance> candidates = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      if (!pickedIndices.contains(i)) {
        int minDistance = Integer.MAX_VALUE;
        for (int pickedIndex : pickedIndices) {
          int distance = Math.abs(i - pickedIndex);
          minDistance = Math.min(minDistance, distance);
        }
        candidates.add(new CandidateWithDistance(i, minDistance));
      }
    }

    Collections.shuffle(candidates, random);
    candidates.sort(Comparator.comparing(CandidateWithDistance::getDistance));

    List<Note> selected = new ArrayList<>();
    Note parent = children.get(0).getParent();
    Set<Integer> picked = pickedChildIndices.get(parent);

    int count = Math.min(remainingBudget, candidates.size());
    for (int i = 0; i < count; i++) {
      CandidateWithDistance candidate = candidates.get(i);
      selected.add(children.get(candidate.getIndex()));
      picked.add(candidate.getIndex());
    }

    return selected;
  }

  private static class CandidateWithDistance {
    private final int index;
    private final int distance;

    CandidateWithDistance(int index, int distance) {
      this.index = index;
      this.distance = distance;
    }

    int getIndex() {
      return index;
    }

    int getDistance() {
      return distance;
    }
  }
}

