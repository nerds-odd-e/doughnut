package com.odde.donut.controllers;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.ResolvedWikiLinkService;
import com.odde.donut.services.WikiLinkRewriteService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text_content")
class TextContentController {
  private final EntityPersister entityPersister;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final NoteService noteService;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public TextContentController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      ResolvedWikiLinkService resolvedWikiLinkService,
      WikiLinkRewriteService wikiLinkRewriteService,
      NoteService noteService,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.noteService = noteService;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  @PatchMapping(path = "/{note}/title")
  @Transactional
  public NoteRealm updateNoteTitle(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateTitleDTO titleDTO)
      throws UnexpectedNoAccessRightException {
    assertReferencedTitleRenameIsUnambiguous(note, titleDTO);
    authorizationService.assertAuthorization(note);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    User viewer = authorizationService.getCurrentUser();
    boolean titleChanged = !Objects.equals(note.getTitle(), titleDTO.getNewTitle());
    if (titleChanged && titleDTO.getReferenceHandling() != null) {
      wikiLinkRewriteService.rewriteInboundWikiLinksForTitleRename(
          note,
          titleDTO.getNewTitle(),
          currentUTCTimestamp,
          viewer,
          titleDTO.getReferenceHandling());
    } else {
      note.setUpdatedAt(currentUTCTimestamp);
      note.setTitle(new DisplayName(titleDTO.getNewTitle()));
      entityPersister.save(note);
    }
    return noteRealmService.build(note, viewer);
  }

  private void assertReferencedTitleRenameIsUnambiguous(Note note, NoteUpdateTitleDTO titleDTO) {
    if (Objects.equals(note.getTitle(), titleDTO.getNewTitle())) {
      return;
    }
    if (!resolvedWikiLinkService.hasInboundResolvedWikiLinkRowsFromNonDeletedReferrers(
        note.getId())) {
      return;
    }
    if (titleDTO.getReferenceHandling() != null) {
      return;
    }
    String message =
        "This note is linked from other notes. Choose how wiki references should be updated"
            + " when renaming.";
    ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
    apiError.add("referenceHandling", message);
    throw new ApiException(apiError);
  }

  @PatchMapping(path = "/{note}/content")
  @Transactional
  public NoteRealm updateNoteContent(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateContentDTO contentDTO)
      throws UnexpectedNoAccessRightException {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(contentDTO.getContent(), canonicalDonutOrigin);
    return updateNote(note, n -> n.replaceContent(document), true, true);
  }

  private NoteRealm updateNote(
      Note note,
      Consumer<Note> updateFunction,
      boolean refreshWikiLinks,
      boolean deleteOrphanImagesAfterSave)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    Set<String> aliasesBefore = aliasLookupKeys(note.getContent());
    note.setUpdatedAt(currentUTCTimestamp);
    updateFunction.accept(note);
    entityPersister.save(note);
    if (deleteOrphanImagesAfterSave) {
      noteService.deleteOrphanImagesForPersistedContent(note);
    }
    if (refreshWikiLinks) {
      User viewer = authorizationService.getCurrentUser();
      resolvedWikiLinkService.refreshForNote(note, viewer);
      if (!aliasesBefore.equals(aliasLookupKeys(note.getContent()))) {
        resolvedWikiLinkService.refreshNotebookScope(note.getNotebook(), viewer);
      }
    }
    return noteRealmService.build(note, authorizationService.getCurrentUser());
  }

  private static Set<String> aliasLookupKeys(String content) {
    return FrontmatterAliases.fromNoteContent(content).stream()
        .map(FrontmatterAliases::normalizedLookupKey)
        .collect(Collectors.toSet());
  }
}
