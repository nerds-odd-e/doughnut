package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteConceptType;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteConstructionService;
import com.odde.donut.services.wikidataApis.WikidataIdWithApi;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class WebNoteCreationService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NotebookRepository notebookRepository;
  private final NoteConstructionService noteConstructionService;
  private final TestabilitySettings testabilitySettings;

  public WebNoteCreationService(
      AcceptedWebChangeService acceptedWebChangeService,
      NotebookRepository notebookRepository,
      NoteConstructionService noteConstructionService,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.notebookRepository = notebookRepository;
    this.noteConstructionService = noteConstructionService;
    this.testabilitySettings = testabilitySettings;
  }

  public NoteRealm createRootNote(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, UnexpectedNoAccessRightException {
    if (wikidataIdWithApi != null || !NoteConceptType.isOrdinary(noteCreation.getContent())) {
      return noteConstructionService.createRootNoteWithWikidataService(
          notebook, noteCreation, user, wikidataIdWithApi);
    }
    return acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Notebook liveNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
          return noteConstructionService.createRootNote(liveNotebook, noteCreation, user);
        },
        realm -> "Add note: " + realm.getNote().getTitle(),
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
