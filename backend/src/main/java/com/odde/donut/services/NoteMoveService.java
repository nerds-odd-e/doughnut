package com.odde.donut.services;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/**
 * Web note-move orchestration: capture inbound references, place the note via {@link
 * NoteMotionService}, then rewrite wiki links. A same-notebook move also carries the note's picture
 * via {@link MovedNotePicture}. Moves run through {@code WebNoteEditService.edit} so the moved tree
 * appends to accepted history (a cross-notebook move in both notebooks); the {@link Consumer}
 * factories here supply the capture-place-rewrite recipe for that boundary.
 */
@Service
public class NoteMoveService {
  private final NoteMotionService noteMotionService;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final AuthorizationService authorizationService;
  private final FolderRepository folderRepository;
  private final MovedNotePicture movedNotePicture;

  public NoteMoveService(
      NoteMotionService noteMotionService,
      WikiLinkRewriteService wikiLinkRewriteService,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite,
      AuthorizationService authorizationService,
      FolderRepository folderRepository,
      MovedNotePicture movedNotePicture) {
    this.noteMotionService = noteMotionService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
    this.authorizationService = authorizationService;
    this.folderRepository = folderRepository;
    this.movedNotePicture = movedNotePicture;
  }

  /**
   * Same-notebook move into an existing folder, as a mutation to run inside the accepted-history
   * edit transaction: capture inbound references, reload the destination folder by id, place and
   * carry the picture, then apply the same-notebook reference rewrite.
   */
  public Consumer<Note> sameNotebookMoveIntoFolder(Integer targetFolderId, Timestamp now) {
    return note -> {
      User user = authorizationService.getCurrentUser();
      Map<Integer, List<String>> inboundReferences =
          wikiLinkRewriteService.captureLiveResolvedInboundReferences(note, user);
      Folder targetFolder = folderRepository.findById(targetFolderId).orElseThrow();
      movedNotePicture.placeWithPicture(
          note,
          targetFolder,
          now,
          () -> noteMotionService.executeMoveIntoFolder(note, targetFolder));
      wikiLinkRelocationRewrite.rewriteInboundWikiLinksForLocationChange(
          note, now, inboundReferences);
    };
  }

  /**
   * Same-notebook move to the notebook root, as a mutation for the accepted-history edit
   * transaction.
   */
  public Consumer<Note> sameNotebookMoveToRoot(Timestamp now) {
    return note -> {
      User user = authorizationService.getCurrentUser();
      Map<Integer, List<String>> inboundReferences =
          wikiLinkRewriteService.captureLiveResolvedInboundReferences(note, user);
      movedNotePicture.placeWithPicture(
          note, null, now, () -> noteMotionService.executeMoveToNotebookRoot(note));
      wikiLinkRelocationRewrite.rewriteInboundWikiLinksForLocationChange(
          note, now, inboundReferences);
    };
  }

  /**
   * Move into a folder of another notebook, as a mutation for an accepted change over both
   * notebooks: the picture stays in the source notebook.
   */
  public Consumer<Note> crossNotebookMoveIntoFolder(
      Integer targetFolderId, Notebook targetNotebook, Timestamp now) {
    return note -> {
      Folder targetFolder = folderRepository.findById(targetFolderId).orElseThrow();
      targetFolder.requireInNotebook(targetNotebook);
      moveCrossNotebook(
          note,
          targetNotebook,
          now,
          () -> noteMotionService.executeMoveIntoFolder(note, targetFolder));
    };
  }

  /** Move to another notebook's root, as a mutation for an accepted change over both notebooks. */
  public Consumer<Note> crossNotebookMoveToRoot(Notebook targetNotebook, Timestamp now) {
    return note ->
        moveCrossNotebook(
            note,
            targetNotebook,
            now,
            () -> noteMotionService.executeMoveToNotebookRoot(note, targetNotebook));
  }

  private void moveCrossNotebook(
      Note note, Notebook targetNotebook, Timestamp now, Runnable place) {
    Notebook oldNotebook = note.getNotebook();
    User user = authorizationService.getCurrentUser();
    Map<Integer, List<String>> inboundReferences =
        wikiLinkRewriteService.captureLiveResolvedInboundReferences(note, user);
    place.run();
    wikiLinkRelocationRewrite.rewriteWikiLinksForCrossNotebookMove(
        note, oldNotebook, targetNotebook, now, user, inboundReferences);
  }
}
