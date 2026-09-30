package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteConstructionService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

/**
 * Reifies one wiki-link property of a note into a relationship note, in one accepted web change;
 * the reverse of {@link RelationReduceService}. Returns the new relationship note's realm.
 */
@Service
public class PropertyReifyService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final WebNoteEditService webNoteEditService;
  private final NoteConstructionService noteConstructionService;
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final TestabilitySettings testabilitySettings;

  public PropertyReifyService(
      AcceptedWebChangeService acceptedWebChangeService,
      WebNoteEditService webNoteEditService,
      NoteConstructionService noteConstructionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.webNoteEditService = webNoteEditService;
    this.noteConstructionService = noteConstructionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.testabilitySettings = testabilitySettings;
  }

  public NoteRealm reifyProperty(Note source, String propertyKey)
      throws UnexpectedNoAccessRightException {
    User viewer = authorizationService.getCurrentUser();
    Integer sourceId = source.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Note relationshipNote =
        acceptedWebChangeService.apply(
            source.getNotebook().getId(),
            () -> {
              Note note = webNoteEditService.requireNote(sourceId);
              authorizationService.assertAuthorization(note);
              return noteConstructionService.reifyPropertyIntoRelationshipNote(
                  note, propertyKey, viewer);
            },
            created -> "Reify property into relationship note: " + created.getTitle(),
            now);
    return noteRealmService.build(relationshipNote, viewer);
  }
}
