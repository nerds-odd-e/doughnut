package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.NotebookZipBuilder;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotebookExportService {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public NotebookExportService(FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  public byte[] exportNotebookAsZip(Notebook notebook) {
    List<ExportFolderRow> folders = NotebookExportRows.folders(folderRepository, notebook);
    List<ExportNoteRow> notes = NotebookExportRows.notes(noteRepository, notebook);
    return NotebookZipBuilder.build(notebook.getReadmeContent(), folders, notes);
  }

  public String exportFileName(Notebook notebook) {
    return notebook.getName() + ".zip";
  }
}
