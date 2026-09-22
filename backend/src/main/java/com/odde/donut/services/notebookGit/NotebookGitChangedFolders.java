package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionRow;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.RowPath;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;

/**
 * The folders a projection change inserted, updated or deleted. A row's previous path composes its
 * container's previous path (as captured for a changed folder, live for an unchanged one); every
 * entry under an updated or deleted folder's previous prefix re-lists under its current prefix, or
 * disappears.
 */
final class NotebookGitChangedFolders {
  private final FolderRepository folderRepository;
  private final Map<Integer, RowPath> previousPaths = new HashMap<>();

  /** Each updated or deleted folder's previous prefix to its current prefix, null once deleted. */
  private final Map<String, String> relocations = new HashMap<>();

  /** Each updated or inserted folder by its current prefix. */
  private final Map<String, Folder> currentFolders = new HashMap<>();

  NotebookGitChangedFolders(FolderRepository folderRepository, NotebookProjectionChange change) {
    this.folderRepository = folderRepository;
    Map<Integer, RowPath> updated = folderRows(change.updated);
    Map<Integer, RowPath> deleted = folderRows(change.deleted);
    previousPaths.putAll(updated);
    previousPaths.putAll(deleted);
    deleted.forEach((id, path) -> relocations.put(previousPrefixOf(path), null));
    updated.forEach(
        (id, path) -> {
          Folder folder = folderRepository.findById(id).orElseThrow();
          String currentPrefix = NotebookGitPortablePath.folderPath(folder);
          relocations.put(previousPrefixOf(path), currentPrefix);
          currentFolders.put(currentPrefix, folder);
        });
    change.inserted.stream()
        .filter(Folder.class::isInstance)
        .map(Folder.class::cast)
        .forEach(folder -> currentFolders.put(NotebookGitPortablePath.folderPath(folder), folder));
  }

  private static Map<Integer, RowPath> folderRows(Map<ProjectionRow, RowPath> rows) {
    return rows.entrySet().stream()
        .filter(entry -> entry.getKey().kind() == Folder.class)
        .collect(Collectors.toMap(entry -> entry.getKey().id(), Map.Entry::getValue));
  }

  Map<String, Folder> currentFolders() {
    return currentFolders;
  }

  /**
   * Where entries left or arrived through folder changes, at their current path: each changed
   * folder's previous container, and each current folder with its container.
   */
  Set<String> touchedDirectories() {
    Set<String> touched = new LinkedHashSet<>();
    relocations
        .keySet()
        .forEach(
            previous ->
                currentPathOf(NotebookGitPortablePath.parentOf(previous)).ifPresent(touched::add));
    currentFolders
        .keySet()
        .forEach(
            current -> {
              touched.add(current);
              touched.add(NotebookGitPortablePath.parentOf(current));
            });
    return touched;
  }

  String previousPathOf(Class<?> kind, RowPath path) {
    String prefix = previousPrefixOf(path.containerId());
    return kind == Note.class
        ? NotebookGitPortablePath.ofNote(prefix, path.name())
        : prefix + path.name();
  }

  /** Every entry at its current path; those under a deleted folder are gone. */
  Map<String, ObjectId> relist(Map<String, ObjectId> previousTree) {
    Map<String, ObjectId> currentTree = new HashMap<>();
    previousTree.forEach(
        (path, blobId) ->
            currentPathOf(path).ifPresent(current -> currentTree.put(current, blobId)));
    return currentTree;
  }

  /**
   * A previous path under its deepest changed folder's current prefix; unchanged when no changed
   * folder contains it, empty when that folder is deleted.
   */
  Optional<String> currentPathOf(String previousPath) {
    Optional<String> previousPrefix =
        relocations.keySet().stream()
            .filter(previousPath::startsWith)
            .max(Comparator.comparing(String::length));
    if (previousPrefix.isEmpty()) return Optional.of(previousPath);
    return Optional.ofNullable(relocations.get(previousPrefix.get()))
        .map(
            currentPrefix -> currentPrefix + previousPath.substring(previousPrefix.get().length()));
  }

  private String previousPrefixOf(RowPath folderPath) {
    return NotebookGitPortablePath.ofFolder(
        previousPrefixOf(folderPath.containerId()), folderPath.name());
  }

  private String previousPrefixOf(Integer folderId) {
    if (folderId == null) return "";
    RowPath path = previousPaths.get(folderId);
    if (path == null) {
      Folder folder = folderRepository.findById(folderId).orElseThrow();
      Integer parentId = folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
      path = new RowPath(parentId, folder.getName());
    }
    return previousPrefixOf(path);
  }
}
