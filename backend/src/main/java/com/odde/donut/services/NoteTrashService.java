package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteTrashService {
  private final WebNoteEditService webNoteEditService;
  private final NoteService noteService;
  private final FolderConstructionService folderConstructionService;
  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;

  public NoteTrashService(
      WebNoteEditService webNoteEditService,
      NoteService noteService,
      FolderConstructionService folderConstructionService,
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings) {
    this.webNoteEditService = webNoteEditService;
    this.noteService = noteService;
    this.folderConstructionService = folderConstructionService;
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
  }

  public Note trash(
      Integer noteId, Integer notebookId, NoteTrashReferenceHandling referenceHandling)
      throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return webNoteEditService.edit(
        noteId,
        notebookId,
        note -> {
          noteService.applyNoteReferenceHandling(
              note, referenceHandling, authorizationService.getCurrentUser());
          Folder trashParent =
              folderConstructionService.ensureTrashParentFor(
                  note.getNotebook(), FolderTrailSegments.fromRootToContainingFolder(note));
          noteMotionService.executeMoveIntoFolderWithAvailableTitle(note, trashParent);
        },
        note -> "Trash note: " + note.getTitle(),
        now);
  }

  public void permanentlyDelete(Integer noteId, Integer notebookId)
      throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    webNoteEditService.edit(
        noteId,
        notebookId,
        note -> {
          if (!note.isTrashed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note is not in trash.");
          }
          noteService.permanentlyRemove(
              note,
              NoteTrashReferenceHandling.LEAVE_DEAD_LINKS,
              authorizationService.getCurrentUser());
        },
        note -> "Permanently delete note: " + note.getTitle(),
        now);
  }
}
