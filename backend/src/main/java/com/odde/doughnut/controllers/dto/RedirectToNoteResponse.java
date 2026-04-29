package com.odde.doughnut.controllers.dto;

public class RedirectToNoteResponse {
  public Integer noteId;
  public Integer notebookId;

  public RedirectToNoteResponse(Integer noteId, Integer notebookId) {
    this.noteId = noteId;
    this.notebookId = notebookId;
  }

  public static RedirectToNoteResponse forNote(int noteId) {
    return new RedirectToNoteResponse(noteId, null);
  }

  public static RedirectToNoteResponse forNotebook(int notebookId) {
    return new RedirectToNoteResponse(null, notebookId);
  }
}
