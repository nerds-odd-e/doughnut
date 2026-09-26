package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NoteFolderAttachment;
import com.odde.donut.services.notebookGit.NotebookGitPortablePath;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Web note-move orchestration: capture inbound references, place the note via {@link
 * NoteMotionService}, then rewrite wiki links. A same-notebook move also carries the note's picture
 * file to its new folder. Same-notebook moves run through {@code WebNoteEditService.edit} so the
 * moved tree appends to accepted history; the {@link Consumer} factories here supply the
 * capture-place-rewrite recipe for that boundary. Cross-notebook moves keep a separate
 * DEFAULT-isolation transaction and are not Git-synchronized in the current slice.
 */
@Service
public class NoteMoveService {
  private final NoteMotionService noteMotionService;
  private final NoteRealmService noteRealmService;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final FolderRepository folderRepository;
  private final NoteFolderAttachment noteFolderAttachment;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;

  public NoteMoveService(
      NoteMotionService noteMotionService,
      NoteRealmService noteRealmService,
      WikiLinkRewriteService wikiLinkRewriteService,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite,
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      FolderRepository folderRepository,
      NoteFolderAttachment noteFolderAttachment,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister) {
    this.noteMotionService = noteMotionService;
    this.noteRealmService = noteRealmService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.folderRepository = folderRepository;
    this.noteFolderAttachment = noteFolderAttachment;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
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
      Optional<NotebookAttachment> picture = pictureToCarry(note, targetFolder);
      noteMotionService.executeMoveIntoFolder(note, targetFolder);
      picture.ifPresent(file -> carry(file, targetFolder));
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
      Optional<NotebookAttachment> picture = pictureToCarry(note, null);
      noteMotionService.executeMoveToNotebookRoot(note);
      picture.ifPresent(file -> carry(file, null));
      wikiLinkRelocationRewrite.rewriteInboundWikiLinksForLocationChange(
          note, now, inboundReferences);
    };
  }

  /**
   * The file the note's {@code image:} names in its own folder, which moves with the note to {@code
   * destinationOrNull}; refused when an entry there already holds its name.
   */
  private Optional<NotebookAttachment> pictureToCarry(Note note, Folder destinationOrNull) {
    if (Objects.equals(folderId(note.getFolder()), folderId(destinationOrNull))) {
      return Optional.empty();
    }
    Optional<NotebookAttachment> picture =
        NoteContentMarkdown.noteImage(note.getContent())
            .filter(NotebookGitPortablePath::isPlainFilename)
            .flatMap(image -> noteFolderAttachment.at(note, image));
    picture
        .flatMap(
            file ->
                folderSiblingNameValidation.entryHolding(
                    note.getNotebook(), destinationOrNull, file.getFilename(), Set.of()))
        .ifPresent(FolderSiblingNameValidation::refuseTaken);
    return picture;
  }

  private void carry(NotebookAttachment file, Folder destinationOrNull) {
    file.setFolder(destinationOrNull);
    entityPersister.merge(file);
  }

  private static Integer folderId(Folder folderOrNull) {
    return folderOrNull == null ? null : folderOrNull.getId();
  }

  @Transactional
  public NoteRealm moveCrossNotebookToFolder(Note source, Folder targetFolder)
      throws UnexpectedNoAccessRightException {
    Notebook oldNotebook = source.getNotebook();
    Notebook targetNotebook = targetFolder.getNotebook();
    User user = authorizationService.getCurrentUser();
    Map<Integer, List<String>> inboundReferences =
        wikiLinkRewriteService.captureLiveResolvedInboundReferences(source, user);
    noteMotionService.executeMoveIntoFolder(source, targetFolder);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    wikiLinkRelocationRewrite.rewriteWikiLinksForCrossNotebookMove(
        source, oldNotebook, targetNotebook, now, user, inboundReferences);
    return noteRealmService.build(source, user);
  }

  @Transactional
  public NoteRealm moveCrossNotebookToNotebookRoot(Note source, Notebook targetNotebook)
      throws UnexpectedNoAccessRightException {
    Notebook oldNotebook = source.getNotebook();
    User user = authorizationService.getCurrentUser();
    Map<Integer, List<String>> inboundReferences =
        wikiLinkRewriteService.captureLiveResolvedInboundReferences(source, user);
    noteMotionService.executeMoveToNotebookRoot(source, targetNotebook);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    wikiLinkRelocationRewrite.rewriteWikiLinksForCrossNotebookMove(
        source, oldNotebook, targetNotebook, now, user, inboundReferences);
    return noteRealmService.build(source, user);
  }
}
