package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.NoteService;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reduces a relationship note into a property of its resolved source note, in one accepted web
 * change, then permanently deletes the relationship note (Git-publication owner via {@link
 * NoteService#permanentlyRemove}). Unlike {@link WebNoteEditService#edit}, the reduced note no
 * longer exists afterward, so this service returns the source note's realm instead.
 */
@Service
public class RelationReduceService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final WebNoteEditService webNoteEditService;
  private final NoteService noteService;
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final TestabilitySettings testabilitySettings;

  public RelationReduceService(
      AcceptedWebChangeService acceptedWebChangeService,
      WebNoteEditService webNoteEditService,
      NoteService noteService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.webNoteEditService = webNoteEditService;
    this.noteService = noteService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.testabilitySettings = testabilitySettings;
  }

  public NoteRealm reduceToSourceProperty(Note relationNote)
      throws UnexpectedNoAccessRightException {
    User viewer = authorizationService.getCurrentUser();
    Set<Integer> notebookIds =
        Stream.of(
                relationNote.getNotebook().getId(),
                noteService.resolveRelationshipSource(relationNote, viewer).getNotebook().getId())
            .collect(Collectors.toUnmodifiableSet());
    Integer relationNoteId = relationNote.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    String commitMessage = "Reduce relationship note: " + relationNote.getTitle();
    Note source =
        acceptedWebChangeService.apply(
            notebookIds,
            locked -> {
              Note note =
                  webNoteEditService.resolveNoteWithinLockedNotebooksOrRepository(
                      locked, relationNoteId);
              authorizationService.assertAuthorization(note);
              Note sourceNote = noteService.reduceRelationNoteToSourceProperty(note, viewer, now);
              if (!notebookIds.contains(note.getNotebook().getId())
                  || !notebookIds.contains(sourceNote.getNotebook().getId())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The relationship or its source note moved to another notebook; retry.");
              }
              noteService.permanentlyRemove(
                  note, NoteTrashReferenceHandling.LEAVE_DEAD_LINKS, viewer);
              return sourceNote;
            },
            ignored -> commitMessage,
            now);
    return noteRealmService.build(source, viewer);
  }
}
