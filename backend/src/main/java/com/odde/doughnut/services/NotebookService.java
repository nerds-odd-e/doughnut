package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookAiAssistantRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookAiAssistantRepository notebookAiAssistantRepository;
  private final WikiSlugPathService wikiSlugPathService;

  public NotebookService(
      EntityPersister entityPersister,
      NotebookAiAssistantRepository notebookAiAssistantRepository,
      WikiSlugPathService wikiSlugPathService) {
    this.entityPersister = entityPersister;
    this.notebookAiAssistantRepository = notebookAiAssistantRepository;
    this.wikiSlugPathService = wikiSlugPathService;
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
      Ownership ownership, User user, Timestamp currentUTCTimestamp, String titleConstructor) {
    Note note =
        ownership.prepareHeadNoteForNewNotebook(user, currentUTCTimestamp, titleConstructor);
    entityPersister.save(note.getNotebook());
    wikiSlugPathService.assignSlugForNewNote(note);
    entityPersister.save(note);
    return note;
  }
}
