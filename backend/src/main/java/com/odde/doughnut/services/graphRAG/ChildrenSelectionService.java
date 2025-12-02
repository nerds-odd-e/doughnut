package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChildrenSelectionService {
  private final Random random = new Random();

  /**
   * Selects children for a parent note using ordered sibling locality logic.
   *
   * <p>Case A (first time): If no children have been selected yet, chooses a random contiguous
   * block of children.
   *
   * <p>Case B (subsequent): If some children have already been selected, prefers children closest
   * to the already-picked ones (nearby siblings).
   *
   * @param parent The parent note
   * @param remainingBudget Number of children that can still be selected (after applying per-depth
   *     cap)
   * @param pickedIndices Indices of children already selected for this parent (empty for first
   *     selection)
   * @param allChildren All available children, ordered by siblingOrder
   * @return List of selected children (up to remainingBudget)
   */
  public List<Note> selectChildren(
      Note parent, int remainingBudget, Set<Integer> pickedIndices, List<Note> allChildren) {
    if (allChildren.isEmpty() || remainingBudget <= 0) {
      return new ArrayList<>();
    }

    if (pickedIndices.isEmpty()) {
      // Case A: First time selecting children - choose random contiguous block
      return selectRandomContiguousBlock(allChildren, remainingBudget);
    } else {
      // Case B: Already selected some children - prefer nearby siblings
      return selectNearbySiblings(allChildren, pickedIndices, remainingBudget);
    }
  }

  /**
   * Case A: Selects a random contiguous block of children.
   *
   * <p>Algorithm:
   * <li>N = total number of children
   * <li>max_start = N - k (where k = remainingBudget)
   * <li>start = random(0..max_start)
   * <li>chosen = [start, start+1, ..., start+k-1]
   *
   * @param allChildren All available children
   * @param k Number of children to select
   * @return List of selected children (contiguous block)
   */
  private List<Note> selectRandomContiguousBlock(List<Note> allChildren, int k) {
    int n = allChildren.size();
    if (k >= n) {
      return new ArrayList<>(allChildren);
    }

    int maxStart = n - k;
    int start = random.nextInt(maxStart + 1);
    return allChildren.subList(start, start + k);
  }

  /**
   * Case B: Selects children closest to already-picked ones (nearby siblings).
   *
   * <p>Algorithm:
   * <li>For each unpicked child index i, compute distance(i) = min(|i - j| for j in pickedIndices)
   * <li>Shuffle candidates to break ties
   * <li>Sort by distance ascending
   * <li>Pick up to remainingBudget
   *
   * @param allChildren All available children
   * @param pickedIndices Indices of already-selected children
   * @param remainingBudget Number of children that can still be selected
   * @return List of selected children (preferring nearby siblings)
   */
  private List<Note> selectNearbySiblings(
      List<Note> allChildren, Set<Integer> pickedIndices, int remainingBudget) {
    // Get all unpicked indices
    List<Integer> unpickedIndices =
        IntStream.range(0, allChildren.size())
            .filter(i -> !pickedIndices.contains(i))
            .boxed()
            .collect(Collectors.toList());

    if (unpickedIndices.isEmpty()) {
      return new ArrayList<>();
    }

    // Calculate distance for each unpicked index
    List<IndexWithDistance> candidates = new ArrayList<>();
    for (Integer index : unpickedIndices) {
      int minDistance =
          pickedIndices.stream()
              .mapToInt(picked -> Math.abs(index - picked))
              .min()
              .orElse(Integer.MAX_VALUE);
      candidates.add(new IndexWithDistance(index, minDistance));
    }

    // Shuffle to break ties randomly
    Collections.shuffle(candidates, random);

    // Sort by distance (ascending) - closest first
    candidates.sort((a, b) -> Integer.compare(a.distance, b.distance));

    // Select up to remainingBudget
    int toSelect = Math.min(remainingBudget, candidates.size());
    List<Note> selected = new ArrayList<>();
    for (int i = 0; i < toSelect; i++) {
      selected.add(allChildren.get(candidates.get(i).index));
    }

    return selected;
  }

  /** Helper class to track index and its distance to already-picked indices. */
  private static class IndexWithDistance {
    final int index;
    final int distance;

    IndexWithDistance(int index, int distance) {
      this.index = index;
      this.distance = distance;
    }
  }
}
