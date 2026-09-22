package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final NotebookRepository notebookRepository;

  NotebookGitDerivedTree(
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      NotebookRepository notebookRepository) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.notebookRepository = notebookRepository;
  }

  /**
   * The accepted tree with every changed note's and attachment's previous path removed, every entry
   * under a changed folder re-listed at its current path with its accepted blob, each inserted or
   * updated note's blob put at its current path, each changed container's {@code README.md}
   * refreshed at its current prefix and the {@code .keep} marker of every touched directory
   * re-evaluated at its current path.
   */
  NotebookGitTreeContent of(
      NotebookProjectionChange change, Map<String, ObjectId> acceptedBlobIds) {
    NotebookGitChangedFolders folders = new NotebookGitChangedFolders(folderRepository, change);
    Set<String> touchedDirectories = new LinkedHashSet<>(folders.touchedDirectories());

    Map<String, ObjectId> previousTree = new HashMap<>(acceptedBlobIds);
    Stream.concat(change.deleted.entrySet().stream(), change.updated.entrySet().stream())
        .filter(entry -> isFile(entry.getKey().kind()))
        .map(entry -> folders.previousPathOf(entry.getKey().kind(), entry.getValue()))
        .forEach(
            previousPath -> {
              previousTree.remove(previousPath);
              folders
                  .currentPathOf(NotebookGitLivePortablePath.directoryOf(previousPath))
                  .ifPresent(touchedDirectories::add);
            });
    Map<String, ObjectId> blobIds = folders.relist(previousTree);
    Map<ObjectId, byte[]> blobs = new HashMap<>();

    currentNoteEntries(change)
        .forEach(
            entry -> {
              put(entry, blobIds, blobs);
              touchedDirectories.add(NotebookGitLivePortablePath.directoryOf(entry.path()));
            });
    readmeContents(change, folders)
        .forEach(
            (prefix, content) -> {
              applyReadme(prefix, content, blobIds, blobs);
              touchedDirectories.add(prefix);
            });
    touchedDirectories.stream()
        .sorted(Comparator.comparing(String::length).reversed())
        .forEach(directory -> applyEmptyDirectoryMarker(directory, blobIds, blobs));
    return new NotebookGitTreeContent(blobIds, blobs);
  }

  private static boolean isFile(Class<?> kind) {
    return kind != Folder.class && kind != Notebook.class;
  }

  /**
   * The current readme content of every container whose row changed, by its current prefix: each
   * inserted or updated folder, and the notebook root when its readme changed.
   */
  private Map<String, String> readmeContents(
      NotebookProjectionChange change, NotebookGitChangedFolders folders) {
    Map<String, String> contents = new HashMap<>();
    folders
        .currentFolders()
        .forEach((prefix, folder) -> contents.put(prefix, folder.getReadmeContent()));
    change.updated.keySet().stream()
        .filter(row -> row.kind() == Notebook.class)
        .findAny()
        .ifPresent(
            row ->
                contents.put(
                    "", notebookRepository.findById(row.id()).orElseThrow().getReadmeContent()));
    return contents;
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

  /** A container's {@code README.md} follows its current readme content. */
  private static void applyReadme(
      String prefix, String content, Map<String, ObjectId> blobIds, Map<ObjectId, byte[]> blobs) {
    blobIds.remove(PortableTreeEntry.readmePath(prefix));
    PortableTreeEntry.ofReadme(prefix, content).ifPresent(entry -> put(entry, blobIds, blobs));
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
    put(PortableTreeEntry.ofText(keep, ""), blobIds, blobs);
  }

  private static void put(
      PortableTreeEntry entry, Map<String, ObjectId> blobIds, Map<ObjectId, byte[]> blobs) {
    NotebookGitTreeContent hashed = NotebookGitTreeContent.of(List.of(entry));
    blobIds.putAll(hashed.blobIds());
    blobs.putAll(hashed.blobs());
  }
}
