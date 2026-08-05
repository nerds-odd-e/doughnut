package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.notebookExport.ExportFolderRow;
import com.odde.doughnut.services.notebookExport.ExportNoteRow;
import com.odde.doughnut.services.notebookExport.NotebookExportFilenames;
import com.odde.doughnut.services.notebookExport.NotebookZipBuilder;
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
    List<ExportFolderRow> folders =
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(
                f ->
                    new ExportFolderRow(
                        f.getId(), f.getParentFolderId(), f.getName(), f.getReadmeContent()))
            .toList();
    List<ExportNoteRow> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(
                n ->
                    new ExportNoteRow(
                        n.getId(),
                        n.getFolder() == null ? null : n.getFolder().getId(),
                        n.getTitle(),
                        n.getContent()))
            .toList();
    return NotebookZipBuilder.build(notebook.getReadmeContent(), folders, notes);
  }

  public String exportFileName(Notebook notebook) {
    return NotebookExportFilenames.sanitize(notebook.getName()) + ".zip";
  }
}
