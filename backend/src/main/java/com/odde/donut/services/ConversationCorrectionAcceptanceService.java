package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.ConversationCorrectionAcceptance;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import jakarta.validation.Validator;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationCorrectionAcceptanceService {
  private final ConversationService conversationService;
  private final AuthorizationService authorizationService;
  private final WebNoteEditService webNoteEditService;
  private final NotebookGitBindingRepository bindingRepository;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final TestabilitySettings testabilitySettings;
  private final Validator validator;

  public ConversationCorrectionAcceptanceService(
      ConversationService conversationService,
      AuthorizationService authorizationService,
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings,
      Validator validator) {
    this.conversationService = conversationService;
    this.authorizationService = authorizationService;
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
    this.validator = validator;
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
    Note subject = conversation.getSubjectNote();
    Timestamp decidedAt = testabilitySettings.getCurrentUTCTimestamp();
    Note saved;
    if (correction.getField() == ConversationNoteCorrection.Field.TITLE) {
      NoteUpdateTitleDTO title = new NoteUpdateTitleDTO();
      title.setNewTitle(review.reviewedValue());
      title.setReferenceHandling(review.referenceHandling());
      var violations = validator.validate(title);
      if (!violations.isEmpty()) {
        ApiError error = new ApiError("Invalid note title.", ApiError.ErrorType.BINDING_ERROR);
        violations.forEach(
            violation -> error.add(violation.getPropertyPath().toString(), violation.getMessage()));
        throw new ApiException(error);
      }
      saved =
          webNoteEditService.saveReviewedTitle(
              subject.getId(),
              subject.getNotebook().getId(),
              title,
              review.baseRevision(),
              decidedAt);
    } else {
      AuthoredNoteDocument document =
          AuthoredNoteContent.prepareDocumentForSave(review.reviewedValue(), canonicalDonutOrigin);
      saved =
          webNoteEditService.saveReviewedContent(
              subject.getId(),
              subject.getNotebook().getId(),
              document,
              review.baseRevision(),
              decidedAt);
    }
    correction.setDecision(ConversationNoteCorrection.Decision.ACCEPTED);
    correction.setBaseRevision(review.baseRevision());
    correction.setReviewedValue(
        correction.getField() == ConversationNoteCorrection.Field.TITLE
            ? saved.getTitle()
            : saved.getContent());
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
