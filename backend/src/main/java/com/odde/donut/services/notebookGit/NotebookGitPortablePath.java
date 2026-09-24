package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Portable paths of live content and of projection rows, walked through folder ancestry. */
final class NotebookGitPortablePath {

  private NotebookGitPortablePath() {}

  static String ofNote(Note note) {
    return ofNote(note.getFolder(), note.getTitle());
  }

  static String ofNote(Folder folder, String title) {
    return ofNote(folderPath(folder), title);
  }

  static String ofNote(String folderPrefix, String title) {
    return folderPrefix + title + ".md";
  }

  static String ofAttachment(NotebookAttachment attachment) {
    return ofAttachment(folderPath(attachment.getFolder()), attachment.getFilename());
  }

  static String ofAttachment(String folderPrefix, String filename) {
    return folderPrefix + filename;
  }

  /** A folder's prefix under its container's prefix. */
  static String ofFolder(String parentPrefix, String name) {
    return parentPrefix + name + "/";
  }

  /** Notebook root ({@code folder == null}) has an empty prefix. */
  static String folderPath(Folder folder) {
    return folder == null ? "" : ofFolder(folderPath(folder.getParentFolder()), folder.getName());
  }

  /**
   * Each folder row's prefix by id, with the notebook root under {@code null}. A folder whose
   * ancestry leaves these rows has none: rows still contained by another notebook's folder are not
   * part of this notebook's tree.
   */
  static Map<Integer, String> folderPrefixes(List<PortableTreeFolderRow> folders) {
    Map<Integer, PortableTreeFolderRow> byId =
        folders.stream().collect(Collectors.toMap(PortableTreeFolderRow::id, Function.identity()));
    Map<Integer, String> prefixes = new HashMap<>();
    prefixes.put(null, "");
    folders.forEach(
        folder ->
            prefixOf(folder.id(), byId).ifPresent(prefix -> prefixes.put(folder.id(), prefix)));
    return prefixes;
  }

  private static Optional<String> prefixOf(
      Integer folderId, Map<Integer, PortableTreeFolderRow> byId) {
    if (folderId == null) return Optional.of("");
    return Optional.ofNullable(byId.get(folderId))
        .flatMap(
            folder ->
                prefixOf(folder.parentFolderId(), byId)
                    .map(parentPrefix -> ofFolder(parentPrefix, folder.name())));
  }

  /** The prefix of the directory holding a file path; empty at the notebook root. */
  static String directoryOf(String path) {
    return path.substring(0, path.lastIndexOf('/') + 1);
  }

  /** The prefix of the directory holding a folder prefix; empty for a root folder. */
  static String parentOf(String prefix) {
    return directoryOf(prefix.substring(0, prefix.length() - 1));
  }
}
