package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentTitleHeading;
import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteCreator;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.NoteExtractionResult;
import com.odde.donut.services.wikidataApis.WikidataIdWithApi;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import com.odde.donut.validators.ReservedReadmeTitles;
import java.io.IOException;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteConstructionService {

  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final NoteRealmService noteRealmService;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final NoteService noteService;
  private final NoteTitlePlacementRules noteTitlePlacementRules;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      NoteRealmService noteRealmService,
      ResolvedWikiLinkService resolvedWikiLinkService,
      NoteService noteService,
      NoteTitlePlacementRules noteTitlePlacementRules,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.noteRealmService = noteRealmService;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.noteService = noteService;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  private Note createNote(Notebook notebook, Folder folderOrNull, String title) {
    throwIfReservedTitle(title);
    noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, folderOrNull, title);
    Note note = new Note();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeNewNote(notebook, ts, title);
    note.setFolder(folderOrNull);
    applyContent(note, null);
    User user = authorizationService.getCurrentUser();
    entityPersister.save(note);
    entityPersister.save(NoteCreator.forNoteAndUser(note, user));
    return note;
  }

  private void persistNoteContent(Note note, String content) {
    applyContent(note, content);
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.save(note);
  }

  /** Prepares {@code content} for save and replaces the note's Markdown and references from it. */
  private void applyContent(Note note, String content) {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin);
    note.replaceContent(document);
  }

  private Note attachWikidataAndRefresh(Note note, WikidataIdWithApi wikidataIdWithApi)
      throws IOException, InterruptedException {
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi
          .fetchWikidataDescription()
          .ifPresent(description -> prependAndPersistWikidataDescription(note, description));
    }
    entityPersister.flush();
    entityPersister.refresh(note);
    return note;
  }

  private void prependAndPersistWikidataDescription(Note note, String description) {
    applyContent(note, NoteLeadingFrontmatter.prependToBody(note.getContent(), description));
    entityPersister.save(note);
  }

  public NoteRealm createRootNoteWithWikidataService(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException {
    Folder folder = null;
    if (noteCreation.getFolderId() != null) {
      folder =
          folderRepository
              .findById(noteCreation.getFolderId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
      if (!folder.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder not in notebook.");
      }
    }
    Note note = createNote(notebook, folder, noteCreation.getNewTitle());
    if (noteCreation.getContent() != null) {
      persistNoteContent(note, noteCreation.getContent());
    }
    note = attachWikidataAndRefresh(note, wikidataIdWithApi);
    noteService.deleteOrphanImagesForPersistedContent(note);
    resolvedWikiLinkService.refreshForNote(note, user);
    return noteRealmService.build(note, user);
  }

  private void throwIfReservedTitle(String title) {
    if (ReservedReadmeTitles.isReserved(title)) {
      ApiError apiError =
          new ApiError(ReservedReadmeTitles.RESERVED_MESSAGE, ApiError.ErrorType.BINDING_ERROR);
      apiError.add("newTitle", ReservedReadmeTitles.RESERVED_MESSAGE);
      throw new ApiException(apiError);
    }
  }

  public NoteRealm createNoteFromExtractedSuggestion(
      Note originalNote, NoteExtractionResult aiResult) {
    User user = authorizationService.getCurrentUser();

    String newNoteContent =
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            aiResult.newNoteTitle, aiResult.newNoteContent);

    Note newNote =
        createNote(originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    persistNoteContent(newNote, newNoteContent);
    persistNoteContent(originalNote, aiResult.updatedOriginalNoteContent);

    noteService.deleteOrphanImagesForPersistedContent(newNote);
    noteService.deleteOrphanImagesForPersistedContent(originalNote);
    resolvedWikiLinkService.refreshForNote(newNote, user);
    resolvedWikiLinkService.refreshForNote(originalNote, user);

    return noteRealmService.build(newNote, user);
  }
}
