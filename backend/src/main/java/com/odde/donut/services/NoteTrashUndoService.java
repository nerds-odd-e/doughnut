package com.odde.donut.services;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteTrashUndoService {
  private final WebNoteEditService webNoteEditService;
  private final NoteMotionService noteMotionService;
  private final FolderRepository folderRepository;
  private final TestabilitySettings testabilitySettings;

  public NoteTrashUndoService(
      WebNoteEditService webNoteEditService,
      NoteMotionService noteMotionService,
      FolderRepository folderRepository,
      TestabilitySettings testabilitySettings) {
    this.webNoteEditService = webNoteEditService;
    this.noteMotionService = noteMotionService;
    this.folderRepository = folderRepository;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder priorFolder(Integer priorFolderId) {
    if (priorFolderId == null) {
      return null;
    }
    return folderRepository
        .findById(priorFolderId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Prior containing folder not found."));
  }

  public Note undoSameNotebook(
      Integer noteId, Integer notebookId, Integer priorFolderId, String priorTitle)
      throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return webNoteEditService.edit(
        noteId,
        notebookId,
        restoredNote ->
            noteMotionService.executePlacement(
                restoredNote, restoredNote.getNotebook(), priorFolder(priorFolderId), priorTitle),
        restoredNote -> "Undo trash: " + restoredNote.getTitle(),
        now);
  }
}
