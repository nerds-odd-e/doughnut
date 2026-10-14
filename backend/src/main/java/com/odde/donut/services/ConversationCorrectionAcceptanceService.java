package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.controllers.dto.ConversationCorrectionAcceptance;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationCorrectionAcceptanceService {
  private final ConversationService conversationService;
  private final AuthorizationService authorizationService;
  private final WebNoteEditService webNoteEditService;
  private final NotebookGitBindingRepository bindingRepository;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final TestabilitySettings testabilitySettings;

  public ConversationCorrectionAcceptanceService(
      ConversationService conversationService,
      AuthorizationService authorizationService,
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings) {
    this.conversationService = conversationService;
    this.authorizationService = authorizationService;
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public ConversationNoteCorrection accept(
      Integer correctionId, ConversationCorrectionAcceptance review)
      throws UnexpectedNoAccessRightException {
    ConversationNoteCorrection correction =
        conversationService.getCorrectionForUpdate(correctionId);
    Conversation conversation = correction.getSourceMessage().getConversation();
    authorizationService.assertAuthorization(conversation);
    if (correction.getDecision() != ConversationNoteCorrection.Decision.PENDING) {
      return correction;
    }
    if (correction.getField() != ConversationNoteCorrection.Field.CONTENT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review a body correction.");
    }
    Note subject = conversation.getSubjectNote();
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(review.reviewedValue(), canonicalDonutOrigin);
    Timestamp decidedAt = testabilitySettings.getCurrentUTCTimestamp();
    Note saved =
        webNoteEditService.saveReviewedContent(
            subject.getId(),
            subject.getNotebook().getId(),
            document,
            review.baseRevision(),
            decidedAt);
    correction.setDecision(ConversationNoteCorrection.Decision.ACCEPTED);
    correction.setBaseRevision(review.baseRevision());
    correction.setReviewedValue(saved.getContent());
    correction.setDecidedBy(authorizationService.getCurrentUser());
    correction.setDecidedAt(decidedAt);
    correction.setAcceptedRevision(
        bindingRepository
            .findByNotebook_Id(saved.getNotebook().getId())
            .map(binding -> binding.getAcceptedGitObjectId())
            .orElse(null));
    return correction;
  }
}
