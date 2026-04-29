package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookAiAssistantRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookAiAssistantRepository notebookAiAssistantRepository;
  private final WikiSlugPathService wikiSlugPathService;
  private final NoteRepository noteRepository;

  public NotebookService(
      EntityPersister entityPersister,
      NotebookAiAssistantRepository notebookAiAssistantRepository,
      WikiSlugPathService wikiSlugPathService,
      NoteRepository noteRepository) {
    this.entityPersister = entityPersister;
    this.notebookAiAssistantRepository = notebookAiAssistantRepository;
    this.wikiSlugPathService = wikiSlugPathService;
    this.noteRepository = noteRepository;
  }

  public NotebookCertificateApproval requestNotebookApproval(Notebook notebook) {
    NotebookCertificateApproval certificateApproval = new NotebookCertificateApproval();
    certificateApproval.setNotebook(notebook);
    entityPersister.save(certificateApproval);
    return certificateApproval;
  }

  public NotebookAiAssistant findByNotebookId(Integer notebookId) {
    return notebookAiAssistantRepository.findByNotebookId(notebookId);
  }

  public NotebookAiAssistant save(NotebookAiAssistant assistant) {
    return notebookAiAssistantRepository.save(assistant);
  }

  public Note createNotebookForOwnership(
      Ownership ownership,
      User user,
      Timestamp currentUTCTimestamp,
      String titleConstructor,
      String description) {
    Note note =
        ownership.prepareHeadNoteForNewNotebook(user, currentUTCTimestamp, titleConstructor);
    Notebook notebook = note.getNotebook();
    if (description != null && !description.isBlank()) {
      notebook.setDescription(description.trim());
    }
    entityPersister.save(notebook);
    wikiSlugPathService.assignSlugForNewNote(note);
    entityPersister.save(note);
    return note;
  }

  public Optional<Note> findOptionalIndexNote(Notebook notebook) {
    if (notebook == null || notebook.getId() == null) {
      return Optional.empty();
    }
    List<Note> found =
        noteRepository.findRootIndexNoteCandidatesForNotebook(
            notebook.getId(), PageRequest.of(0, 1));
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
  }
}
