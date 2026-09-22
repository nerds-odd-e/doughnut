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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  NotebookGitDerivedTree(NoteRepository noteRepository, FolderRepository folderRepository) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
  }

  /**
   * The accepted tree with each inserted or updated note's blob put at its path, each deleted
   * note's path removed and the {@code .keep} marker of every touched directory re-evaluated; empty
   * when the change holds any other kind of row change, which the caller still assembles in full.
   */
  Optional<NotebookGitTreeContent> of(
      NotebookProjectionChange change, Map<String, ObjectId> acceptedBlobIds) {
    Stream<ProjectionRow> rows =
        Stream.concat(change.updated.keySet().stream(), change.deleted.keySet().stream());
    if (rows.anyMatch(row -> row.kind() != Note.class)
        || !change.inserted.stream().allMatch(Note.class::isInstance)) {
      return Optional.empty();
    }
    List<PortableTreeEntry> added = new ArrayList<>();
    for (Map.Entry<ProjectionRow, RowPath> update : change.updated.entrySet()) {
      Note note = noteRepository.findById(update.getKey().id()).orElseThrow();
      if (!update.getValue().equals(pathOf(note))) {
        return Optional.empty();
      }
      added.add(entryOf(note));
    }
    change.inserted.forEach(inserted -> added.add(entryOf((Note) inserted)));
    List<String> removed = change.deleted.values().stream().map(this::notePathOf).toList();

    Map<String, ObjectId> blobIds = new HashMap<>(acceptedBlobIds);
    removed.forEach(blobIds::remove);
    NotebookGitTreeContent addedContent = NotebookGitTreeContent.of(added);
    blobIds.putAll(addedContent.blobIds());
    Map<ObjectId, byte[]> blobs = new HashMap<>(addedContent.blobs());
    Set<String> touchedDirectories = new LinkedHashSet<>();
    Stream.concat(removed.stream(), added.stream().map(PortableTreeEntry::path))
        .map(path -> path.substring(0, path.lastIndexOf('/') + 1))
        .forEach(touchedDirectories::add);
    touchedDirectories.forEach(directory -> applyEmptyDirectoryMarker(directory, blobIds, blobs));
    return Optional.of(new NotebookGitTreeContent(blobIds, blobs));
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

  private static PortableTreeEntry entryOf(Note note) {
    return PortableTreeEntry.ofNote(NotebookGitLivePortablePath.ofNote(note), note.getContent());
  }

  private String notePathOf(RowPath path) {
    Folder folder =
        path.containerId() == null
            ? null
            : folderRepository.findById(path.containerId()).orElseThrow();
    return NotebookGitLivePortablePath.ofNote(folder, path.name());
  }

  private static RowPath pathOf(Note note) {
    Integer folderId = note.getFolder() == null ? null : note.getFolder().getId();
    return new RowPath(folderId, note.getTitle());
  }
}
