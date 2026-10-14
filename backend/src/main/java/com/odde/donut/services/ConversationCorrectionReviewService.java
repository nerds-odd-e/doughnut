package com.odde.donut.services;

import com.odde.donut.controllers.dto.ConversationCorrectionReview;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationCorrectionReviewService {
  private final ConversationService conversationService;
  private final AuthorizationService authorizationService;
  private final NotebookGitBindingRepository bindingRepository;
  private final EntityPersister entityPersister;
  private final NoteRealmService noteRealmService;

  public ConversationCorrectionReviewService(
      ConversationService conversationService,
      AuthorizationService authorizationService,
      NotebookGitBindingRepository bindingRepository,
      EntityPersister entityPersister,
      NoteRealmService noteRealmService) {
    this.conversationService = conversationService;
    this.authorizationService = authorizationService;
    this.bindingRepository = bindingRepository;
    this.entityPersister = entityPersister;
    this.noteRealmService = noteRealmService;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ConversationCorrectionReview loadCurrentReview(Integer correctionId)
      throws UnexpectedNoAccessRightException {
    var correction = conversationService.getCorrectionForUpdate(correctionId);
    var conversation = correction.getSourceMessage().getConversation();
    authorizationService.assertAuthorization(conversation);
    Note subject = conversation.getSubjectNote();
    Integer notebookId = subject.getNotebook().getId();
    var binding = bindingRepository.findByNotebookIdForUpdate(notebookId);
    entityPersister.refresh(subject);
    if (!subject.getNotebook().getId().equals(notebookId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "The note moved. Load the current note again.");
    }
    authorizationService.assertReadAuthorization(subject);
    return new ConversationCorrectionReview(
        noteRealmService.build(subject, authorizationService.getCurrentUser()),
        binding.map(value -> value.getAcceptedGitObjectId()).orElse(null));
  }
}
