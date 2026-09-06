package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteCreator;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import com.odde.donut.validators.ReservedReadmeTitles;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class NoteFactory {

  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final EntityPersister entityPersister;
  private final NoteTitlePlacementRules noteTitlePlacementRules;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public NoteFactory(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NoteTitlePlacementRules noteTitlePlacementRules,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  public Note create(Notebook notebook, Folder folderOrNull, String title) {
    throwIfReservedTitle(title);
    noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, folderOrNull, title);
    Note note = new Note();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeNewNote(notebook, ts, title);
    note.setFolder(folderOrNull);
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(null, canonicalDonutOrigin);
    note.replaceContent(document);
    User user = authorizationService.getCurrentUser();
    entityPersister.save(note);
    entityPersister.save(NoteCreator.forNoteAndUser(note, user));
    return note;
  }

  private void throwIfReservedTitle(String title) {
    if (ReservedReadmeTitles.isReserved(title)) {
      ApiError apiError =
          new ApiError(ReservedReadmeTitles.RESERVED_MESSAGE, ApiError.ErrorType.BINDING_ERROR);
      apiError.add("newTitle", ReservedReadmeTitles.RESERVED_MESSAGE);
      throw new ApiException(apiError);
    }
  }
}
