package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookGitStateLoader {
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookRepository notebookRepository;
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public NotebookGitStateLoader(
      NotebookGitBindingRepository bindingRepository,
      NotebookRepository notebookRepository,
      FolderRepository folderRepository,
      NoteRepository noteRepository) {
    this.bindingRepository = bindingRepository;
    this.notebookRepository = notebookRepository;
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  public Optional<LockedNotebookState> findByNotebookIdForUpdate(Integer notebookId) {
    return bindingRepository
        .findByNotebookIdForUpdate(notebookId)
        .map(binding -> loadNotebookState(binding, notebookId));
  }

  private LockedNotebookState loadNotebookState(NotebookGitBinding binding, Integer notebookId) {
    Notebook notebook =
        notebookRepository
            .findById(notebookId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    List<ExportFolderRow> folders = NotebookExportRows.folders(folderRepository, notebook);
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebookId);
    return new LockedNotebookState(binding, notebook, folders, liveNotes);
  }

  public record LockedNotebookState(
      NotebookGitBinding binding,
      Notebook notebook,
      List<ExportFolderRow> folders,
      List<Note> liveNotes) {}
}
