package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Migration-only title collision detection and qualifier disambiguation within notebook+folder. */
public final class TitleAliasMigrationCollisionPolicy {

  private TitleAliasMigrationCollisionPolicy() {}

  public record NotePlacement(
      int noteId, int notebookId, Integer folderId, String basePlannedTitle) {}

  public record Member(int noteId, String resolvedTitle) {}

  public record CollisionGroup(
      int notebookId, Integer folderId, String basePlannedTitle, List<Member> members) {}

  private record GroupKey(int notebookId, Integer folderId, String basePlannedTitleLower) {
    static GroupKey from(NotePlacement placement) {
      return new GroupKey(
          placement.notebookId(),
          placement.folderId(),
          placement.basePlannedTitle().toLowerCase(Locale.ROOT));
    }
  }

  public static Map<Integer, String> resolve(List<NotePlacement> placements) {
    Map<Integer, String> resolved = new LinkedHashMap<>();
    groupedByCollisionKey(placements)
        .forEach(
            (key, group) -> {
              List<NotePlacement> ordered =
                  group.stream().sorted(Comparator.comparingInt(NotePlacement::noteId)).toList();
              for (int i = 0; i < ordered.size(); i++) {
                NotePlacement placement = ordered.get(i);
                resolved.put(
                    placement.noteId(),
                    i == 0
                        ? placement.basePlannedTitle()
                        : disambiguatedTitle(placement.basePlannedTitle(), i));
              }
            });
    return Map.copyOf(resolved);
  }

  public static Set<Integer> collisionNoteIds(List<NotePlacement> placements) {
    return collisionGroups(placements).stream()
        .flatMap(group -> group.members().stream())
        .map(Member::noteId)
        .collect(Collectors.toUnmodifiableSet());
  }

  public static List<CollisionGroup> collisionGroups(List<NotePlacement> placements) {
    List<CollisionGroup> groups = new ArrayList<>();
    Map<Integer, String> resolved = resolve(placements);
    groupedByCollisionKey(placements)
        .forEach(
            (key, group) -> {
              if (group.size() < 2) {
                return;
              }
              List<NotePlacement> ordered =
                  group.stream().sorted(Comparator.comparingInt(NotePlacement::noteId)).toList();
              List<Member> members =
                  ordered.stream()
                      .map(
                          p ->
                              new Member(
                                  p.noteId(),
                                  resolved.getOrDefault(p.noteId(), p.basePlannedTitle())))
                      .toList();
              groups.add(
                  new CollisionGroup(
                      key.notebookId(),
                      key.folderId(),
                      ordered.getFirst().basePlannedTitle(),
                      List.copyOf(members)));
            });
    return List.copyOf(groups);
  }

  private static Map<GroupKey, List<NotePlacement>> groupedByCollisionKey(
      List<NotePlacement> placements) {
    return placements.stream()
        .collect(Collectors.groupingBy(GroupKey::from, LinkedHashMap::new, Collectors.toList()));
  }

  static String disambiguatedTitle(String basePlannedTitle, int collisionIndex) {
    NoteTitle noteTitle = new NoteTitle(basePlannedTitle);
    String stem = titleStemWithoutQualifier(noteTitle);
    String qualifier =
        noteTitle
            .getQualifier()
            .map(TitleFragment::stem)
            .map(q -> q + " " + collisionIndex)
            .orElse(String.valueOf(collisionIndex));
    return stem + " (" + qualifier + ")";
  }

  public static boolean memberTitleMatchesCollisionBase(
      String memberTitle, String basePlannedTitle, int maxCollisionIndex) {
    if (memberTitle.equals(basePlannedTitle)) {
      return true;
    }
    for (int i = 1; i <= maxCollisionIndex; i++) {
      if (memberTitle.equals(disambiguatedTitle(basePlannedTitle, i))) {
        return true;
      }
    }
    return false;
  }

  private static String titleStemWithoutQualifier(NoteTitle noteTitle) {
    List<TitleFragment> segments = noteTitle.getAliasSegmentsInOrder();
    if (segments.isEmpty()) {
      return "";
    }
    StringBuilder title = new StringBuilder();
    for (TitleFragment segment : segments) {
      if (!title.isEmpty()) {
        title.append('／');
      }
      title.append(segment.suffixMarker() ? "~" : "").append(segment.stem());
    }
    return title.toString();
  }
}
