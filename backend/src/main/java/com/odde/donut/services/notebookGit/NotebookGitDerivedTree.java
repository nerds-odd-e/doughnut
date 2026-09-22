package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionRow;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.RowPath;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Derives a web commit's Portable tree from the accepted head's path-to-blob map and the projection
 * change the operation flushed, so only the changed rows are rendered and hashed.
 */
@Component
class NotebookGitDerivedTree {
  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;

  NotebookGitDerivedTree(NoteRepository noteRepository, FolderRepository folderRepository) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
  }

  /**
   * The accepted tree with every changed note's and attachment's previous path removed, every entry
   * under a changed folder re-listed at its current path with its accepted blob, each inserted or
   * updated note's blob put at its current path and the {@code .keep} marker of every touched
   * directory re-evaluated at its current path; empty when the change holds a notebook readme or an
   * attachment insert or update, which the caller still assembles in full.
   */
  Optional<NotebookGitTreeContent> of(
      NotebookProjectionChange change, Map<String, ObjectId> acceptedBlobIds) {
    Stream<Class<?>> presentKinds =
        Stream.concat(
            change.updated.keySet().stream().map(ProjectionRow::kind),
            change.inserted.stream().map(Object::getClass));
    if (!presentKinds.allMatch(kind -> kind == Note.class || kind == Folder.class)) {
      return Optional.empty();
    }
    ChangedFolders folders = new ChangedFolders(change);
    Set<String> touchedDirectories = new LinkedHashSet<>(folders.touchedDirectories());

    Map<String, ObjectId> previousTree = new HashMap<>(acceptedBlobIds);
    Stream.concat(change.deleted.entrySet().stream(), change.updated.entrySet().stream())
        .filter(entry -> entry.getKey().kind() != Folder.class)
        .map(entry -> folders.previousPathOf(entry.getKey().kind(), entry.getValue()))
        .forEach(
            previousPath -> {
              previousTree.remove(previousPath);
              folders.currentPathOf(directoryOf(previousPath)).ifPresent(touchedDirectories::add);
            });
    Map<String, ObjectId> blobIds = folders.relist(previousTree);

    List<PortableTreeEntry> added = currentNoteEntries(change);
    NotebookGitTreeContent addedContent = NotebookGitTreeContent.of(added);
    blobIds.putAll(addedContent.blobIds());
    Map<ObjectId, byte[]> blobs = new HashMap<>(addedContent.blobs());
    added.forEach(entry -> touchedDirectories.add(directoryOf(entry.path())));

    touchedDirectories.stream()
        .sorted(Comparator.comparing(String::length).reversed())
        .forEach(directory -> applyEmptyDirectoryMarker(directory, blobIds, blobs));
    return Optional.of(new NotebookGitTreeContent(blobIds, blobs));
  }

  /** The current file of every note the change updated or inserted. */
  private List<PortableTreeEntry> currentNoteEntries(NotebookProjectionChange change) {
    Stream<Note> updated =
        change.updated.keySet().stream()
            .filter(row -> row.kind() == Note.class)
            .map(row -> noteRepository.findById(row.id()).orElseThrow());
    Stream<Note> inserted =
        change.inserted.stream().filter(Note.class::isInstance).map(Note.class::cast);
    return Stream.concat(updated, inserted)
        .map(
            note ->
                PortableTreeEntry.ofNote(
                    NotebookGitLivePortablePath.ofNote(note), note.getContent()))
        .toList();
  }

  /**
   * A represented folder with no other entry holds {@code .keep}; the notebook root never does.
   * Same rule as the full assembly's {@code PortableTreeSnapshot}.
   */
  private static void applyEmptyDirectoryMarker(
      String directory, Map<String, ObjectId> blobIds, Map<ObjectId, byte[]> blobs) {
    if (directory.isEmpty()) return;
    String keep = directory + ".keep";
    boolean empty =
        blobIds.keySet().stream()
            .noneMatch(path -> path.startsWith(directory) && !path.equals(keep));
    if (!empty) {
      blobIds.remove(keep);
      return;
    }
    NotebookGitTreeContent marker =
        NotebookGitTreeContent.of(List.of(PortableTreeEntry.ofText(keep, "")));
    blobIds.putAll(marker.blobIds());
    blobs.putAll(marker.blobs());
  }

  private static String directoryOf(String path) {
    return path.substring(0, path.lastIndexOf('/') + 1);
  }

  private static String parentOf(String prefix) {
    return directoryOf(prefix.substring(0, prefix.length() - 1));
  }

  private static Map<Integer, RowPath> folderRows(Map<ProjectionRow, RowPath> rows) {
    return rows.entrySet().stream()
        .filter(entry -> entry.getKey().kind() == Folder.class)
        .collect(Collectors.toMap(entry -> entry.getKey().id(), Map.Entry::getValue));
  }

  /**
   * The folders the change inserted, updated or deleted. A row's previous path composes its
   * container's previous path (as captured for a changed folder, live for an unchanged one); every
   * entry under an updated or deleted folder's previous prefix re-lists under its current prefix,
   * or disappears.
   */
  private final class ChangedFolders {
    private final Map<Integer, RowPath> previousPaths = new HashMap<>();

    /**
     * Each updated or deleted folder's previous prefix to its current prefix, null once deleted.
     */
    private final Map<String, String> relocations = new HashMap<>();

    /** Where each updated or inserted folder now is. */
    private final List<String> currentPrefixes = new ArrayList<>();

    ChangedFolders(NotebookProjectionChange change) {
      Map<Integer, RowPath> updated = folderRows(change.updated);
      Map<Integer, RowPath> deleted = folderRows(change.deleted);
      previousPaths.putAll(updated);
      previousPaths.putAll(deleted);
      deleted.forEach((id, path) -> relocations.put(previousPrefixOf(path), null));
      updated.forEach(
          (id, path) -> {
            Folder folder = folderRepository.findById(id).orElseThrow();
            String currentPrefix = NotebookGitLivePortablePath.folderPath(folder);
            relocations.put(previousPrefixOf(path), currentPrefix);
            currentPrefixes.add(currentPrefix);
          });
      change.inserted.stream()
          .filter(Folder.class::isInstance)
          .map(Folder.class::cast)
          .map(NotebookGitLivePortablePath::folderPath)
          .forEach(currentPrefixes::add);
    }

    /**
     * Where entries left or arrived through folder changes, at their current path: each changed
     * folder's previous container, and each current folder with its container.
     */
    Set<String> touchedDirectories() {
      Set<String> touched = new LinkedHashSet<>();
      relocations
          .keySet()
          .forEach(previous -> currentPathOf(parentOf(previous)).ifPresent(touched::add));
      currentPrefixes.forEach(
          current -> {
            touched.add(current);
            touched.add(parentOf(current));
          });
      return touched;
    }

    String previousPathOf(Class<?> kind, RowPath path) {
      String prefix = previousPrefixOf(path.containerId());
      return kind == Note.class
          ? NotebookGitLivePortablePath.ofNote(prefix, path.name())
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
              currentPrefix ->
                  currentPrefix + previousPath.substring(previousPrefix.get().length()));
    }

    private String previousPrefixOf(RowPath folderPath) {
      return previousPrefixOf(folderPath.containerId()) + folderPath.name() + "/";
    }

    private String previousPrefixOf(Integer folderId) {
      if (folderId == null) return "";
      RowPath path = previousPaths.get(folderId);
      if (path == null) {
        Folder folder = folderRepository.findById(folderId).orElseThrow();
        Integer parentId =
            folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
        path = new RowPath(parentId, folder.getName());
      }
      return previousPrefixOf(path);
    }
  }
}
