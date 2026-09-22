package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionRow;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.RowPath;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Derives a web commit's Portable tree from the accepted head's path-to-blob map and the projection
 * change the operation flushed, so only the changed rows are rendered and hashed.
 */
@Component
class NotebookGitDerivedTree {
  private final NoteRepository noteRepository;

  NotebookGitDerivedTree(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  /**
   * The accepted tree with each updated note's blob replaced in place; empty when the change holds
   * any other kind of row change, which the caller still assembles in full.
   */
  Optional<NotebookGitTreeContent> of(
      NotebookProjectionChange change, Map<String, ObjectId> acceptedBlobIds) {
    if (!change.inserted.isEmpty() || !change.deleted.isEmpty()) {
      return Optional.empty();
    }
    List<PortableTreeEntry> replaced = new ArrayList<>();
    for (Map.Entry<ProjectionRow, RowPath> update : change.updated.entrySet()) {
      if (update.getKey().kind() != Note.class) {
        return Optional.empty();
      }
      Note note = noteRepository.findById(update.getKey().id()).orElseThrow();
      if (!update.getValue().equals(pathOf(note))) {
        return Optional.empty();
      }
      replaced.add(
          PortableTreeEntry.ofNote(NotebookGitLivePortablePath.ofNote(note), note.getContent()));
    }
    NotebookGitTreeContent replacedContent = NotebookGitTreeContent.of(replaced);
    Map<String, ObjectId> blobIds = new HashMap<>(acceptedBlobIds);
    blobIds.putAll(replacedContent.blobIds());
    return Optional.of(new NotebookGitTreeContent(blobIds, replacedContent.blobs()));
  }

  private static RowPath pathOf(Note note) {
    Integer folderId = note.getFolder() == null ? null : note.getFolder().getId();
    return new RowPath(folderId, note.getTitle());
  }
}
