package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentTitleHeading;
import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.NoteExtractionResult;
import com.odde.donut.services.wikidataApis.WikidataIdWithApi;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import java.io.IOException;
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
  private final NoteReferenceService noteReferenceService;
  private final NoteService noteService;
  private final NoteFactory noteFactory;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      NoteRealmService noteRealmService,
      NoteReferenceService noteReferenceService,
      NoteService noteService,
      NoteFactory noteFactory,
      CanonicalDonutOrigin canonicalDonutOrigin,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.noteRealmService = noteRealmService;
    this.noteReferenceService = noteReferenceService;
    this.noteService = noteService;
    this.noteFactory = noteFactory;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
  }

  private Note persistNoteContent(Note note, String content) {
    applyContent(note, content);
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    return entityPersister.save(note);
  }

  /** Prepares {@code content} for save and replaces the note's Markdown and references from it. */
  private void applyContent(Note note, String content) {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin);
    note.replaceContent(document);
  }

  private void prependAndPersistWikidataDescription(Note note, String description) {
    applyContent(note, NoteLeadingFrontmatter.prependToBody(note.getContent(), description));
    entityPersister.save(note);
  }

  private Note buildNote(Notebook notebook, NoteCreationDTO noteCreation) {
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
    Note note = noteFactory.create(notebook, folder, noteCreation.getNewTitle());
    if (noteCreation.getContent() != null) {
      persistNoteContent(note, noteCreation.getContent());
    }
    return note;
  }

  /** Final refresh, image cleanup, reference indexing and response construction. */
  private NoteRealm finalizeAndRespond(Note note, User user) {
    entityPersister.flush();
    entityPersister.refresh(note);
    noteService.deleteOrphanImagesForPersistedContent(note);
    noteReferenceService.refreshDerivedIndexesForNote(note);
    return noteRealmService.build(note, user);
  }

  /** Synchronous construction: no external enrichment, so no IO/interruption contract. */
  public NoteRealm createRootNote(Notebook notebook, NoteCreationDTO noteCreation, User user) {
    Note note = buildNote(notebook, noteCreation);
    return finalizeAndRespond(note, user);
  }

  public NoteRealm createRootNoteWithWikidataService(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException {
    Note note = buildNote(notebook, noteCreation);
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi
          .fetchWikidataDescription()
          .ifPresent(description -> prependAndPersistWikidataDescription(note, description));
    }
    return finalizeAndRespond(note, user);
  }

  public NoteRealm createNoteFromExtractedSuggestion(
      Note originalNote, NoteExtractionResult aiResult) {
    User user = authorizationService.getCurrentUser();

    String newNoteContent =
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            aiResult.newNoteTitle, aiResult.newNoteContent);

    Note newNote =
        noteFactory.create(
            originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    persistAuthoredContent(newNote, newNoteContent);
    persistAuthoredContent(originalNote, aiResult.updatedOriginalNoteContent);

    return noteRealmService.build(newNote, user);
  }

  private void persistAuthoredContent(Note note, String content) {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin);
    authoredNoteDocumentPersistence.persist(
        note, document, testabilitySettings.getCurrentUTCTimestamp());
  }
}
