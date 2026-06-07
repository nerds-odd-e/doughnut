package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecalledNote {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private NoteTopology noteTopology;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;

  private List<Folder> ancestorFolders;

  private String propertyKey;

  public static RecalledNote from(Note note, String propertyKey) {
    if (note == null) {
      return null;
    }
    RecalledNote recalledNote = new RecalledNote();
    recalledNote.setNoteTopology(note.getNoteTopology());
    recalledNote.setNotebookId(note.getNotebook().getId());
    recalledNote.setAncestorFolders(FolderTrailSegments.fromRootToContainingFolder(note));
    String key = propertyKey;
    recalledNote.setPropertyKey(key == null || key.isEmpty() ? null : key);
    return recalledNote;
  }
}
