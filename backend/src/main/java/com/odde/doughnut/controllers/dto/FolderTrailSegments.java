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
        .map(Folder::getName)
        .collect(Collectors.joining(" / "));
  }

  public static List<Folder> fromRootToContainingFolder(Note note) {
    Folder folder = note.getFolder();
    if (folder == null) {
      return List.of();
    }
    return fromRootToFolder(folder);
  }

  /** Outermost folder first, from notebook root through {@code folder} (inclusive). */
  public static List<Folder> fromRootToFolder(Folder folder) {
    List<Folder> leafToRoot = new ArrayList<>();
    for (Folder f = folder; f != null; f = f.getParentFolder()) {
      leafToRoot.add(f);
    }
    Collections.reverse(leafToRoot);
    return List.copyOf(leafToRoot);
  }

  /**
   * Outermost first, from notebook root through the parent of {@code folder}. Empty when {@code
   * folder} is at notebook root.
   */
  public static List<Folder> ancestorsFromRootToParent(Folder folder) {
    Folder parent = folder.getParentFolder();
    if (parent == null) {
      return List.of();
    }
    return fromRootToFolder(parent);
  }
}
