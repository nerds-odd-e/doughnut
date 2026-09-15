package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

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
      Integer noteId,
      Integer notebookId,
      NoteDeleteReferenceHandling referenceHandling,
      String sourcePropertyKey)
      throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return webNoteEditService.edit(
        noteId,
        notebookId,
        note -> {
          noteService.applyNoteDeleteReferenceHandling(
              note, referenceHandling, sourcePropertyKey, authorizationService.getCurrentUser());
          Folder trashParent =
              folderConstructionService.ensureTrashParentFor(
                  note.getNotebook(), FolderTrailSegments.fromRootToContainingFolder(note));
          noteMotionService.executeMoveIntoFolderWithAvailableTitle(note, trashParent);
        },
        note -> "Trash note: " + note.getTitle(),
        now);
  }
}
