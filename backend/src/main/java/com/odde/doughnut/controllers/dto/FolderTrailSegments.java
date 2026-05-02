package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Folder path from notebook root to the note's containing folder (outermost first). */
public final class FolderTrailSegments {
  private FolderTrailSegments() {}

  /** Outer-most folder first, separated by {@code " / "} (empty when the note has no folder). */
  public static String crumbPathJoinedBySlashSpace(Note note) {
    return fromRootToContainingFolder(note).stream()
        .map(FolderTrailSegment::name)
        .collect(Collectors.joining(" / "));
  }

  public static List<FolderTrailSegment> fromRootToContainingFolder(Note note) {
    Folder folder = note.getFolder();
    if (folder == null) {
      return List.of();
    }
    List<Folder> leafToRoot = new ArrayList<>();
    for (Folder f = folder; f != null; f = f.getParentFolder()) {
      leafToRoot.add(f);
    }
    Collections.reverse(leafToRoot);
    return leafToRoot.stream().map(FolderTrailSegment::from).toList();
  }
}
