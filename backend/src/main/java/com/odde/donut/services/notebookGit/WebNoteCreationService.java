package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteConstructionService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.ai.NoteExtractionResult;
import com.odde.donut.services.wikidataApis.WikidataIdWithApi;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;

/** Creates notes from the web, each creation in one accepted web change of its notebook. */
@Service
public class WebNoteCreationService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NotebookRepository notebookRepository;
  private final WebNoteEditService webNoteEditService;
  private final NoteConstructionService noteConstructionService;
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final TestabilitySettings testabilitySettings;

  public WebNoteCreationService(
      AcceptedWebChangeService acceptedWebChangeService,
      NotebookRepository notebookRepository,
      WebNoteEditService webNoteEditService,
      NoteConstructionService noteConstructionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.notebookRepository = notebookRepository;
    this.webNoteEditService = webNoteEditService;
    this.noteConstructionService = noteConstructionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.testabilitySettings = testabilitySettings;
  }

  public NoteRealm createRootNote(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, UnexpectedNoAccessRightException {
    Optional<String> wikidataDescription =
        wikidataIdWithApi == null ? Optional.empty() : wikidataIdWithApi.fetchWikidataDescription();
    return acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Notebook liveNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
          return noteConstructionService.createRootNote(
              liveNotebook, noteCreation, user, wikidataDescription);
        },
        realm -> "Add note: " + realm.getNote().getTitle(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  /**
   * Reifies one wiki-link property of {@code source} into a relationship note; the reverse of
   * {@link RelationReduceService}. Returns the relationship note's realm.
   */
  public NoteRealm reifyProperty(Note source, String propertyKey)
      throws UnexpectedNoAccessRightException {
    User viewer = authorizationService.getCurrentUser();
    return createFrom(
        source,
        note ->
            noteConstructionService.reifyPropertyIntoRelationshipNote(note, propertyKey, viewer),
        created -> "Reify property into relationship note: " + created.getTitle(),
        viewer);
  }

  /** Creates the note extracted from {@code original} and rewrites the original. */
  public NoteRealm createExtractedNote(Note original, NoteExtractionResult extraction)
      throws UnexpectedNoAccessRightException {
    return createFrom(
        original,
        note -> noteConstructionService.createNoteFromExtractedSuggestion(note, extraction),
        created -> "Extract note: " + created.getTitle(),
        authorizationService.getCurrentUser());
  }

  private NoteRealm createFrom(
      Note source,
      UnaryOperator<Note> construction,
      Function<Note, String> commitMessage,
      User viewer)
      throws UnexpectedNoAccessRightException {
    Integer sourceId = source.getId();
    Note created =
        acceptedWebChangeService.apply(
            source.getNotebook().getId(),
            () -> {
              Note note = webNoteEditService.requireNote(sourceId);
              authorizationService.assertAuthorization(note);
              return construction.apply(note);
            },
            commitMessage,
            testabilitySettings.getCurrentUTCTimestamp());
    return noteRealmService.build(created, viewer);
  }
}
