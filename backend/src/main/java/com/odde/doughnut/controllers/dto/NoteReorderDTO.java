package com.odde.doughnut.controllers.dto;

public class NoteReorderDTO {
  /** {@code null} means notebook root (notes with no folder in that notebook). */
  public Integer folderId;

  /** {@code null} means first in the placement scope; otherwise id of the note to place after. */
  public Integer afterNoteId;
}
