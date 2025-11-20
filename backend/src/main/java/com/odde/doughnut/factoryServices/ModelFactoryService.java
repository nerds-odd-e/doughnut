package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.services.AuthorizationService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
  @Autowired public NoteReviewRepository noteReviewRepository;
  @Autowired public NoteRepository noteRepository;
  @Autowired public UserRepository userRepository;
  @Autowired public BazaarNotebookRepository bazaarNotebookRepository;
  @Autowired public MemoryTrackerRepository memoryTrackerRepository;
  @Autowired public CircleRepository circleRepository;
  @Autowired public FailureReportRepository failureReportRepository;
  @Autowired public GlobalSettingRepository globalSettingRepository;
  @Autowired public AssessmentAttemptRepository assessmentAttemptRepository;
  @Autowired public EntityPersister entityPersister;
  @Autowired public EntityManager entityManager;
  @Autowired public NotebookRepository notebookRepository;
  @Autowired public CertificateRepository certificateRepository;
  @Autowired public ConversationRepository conversationRepository;
  @Autowired public ConversationMessageRepository conversationMessageRepository;
  @Autowired public NotebookCertificateApprovalRepository notebookCertificateApprovalRepository;
  @Autowired public RecallPromptRepository recallPromptRepository;

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

  @Autowired public NotebookAiAssistantRepository notebookAiAssistantRepository;

  @Autowired public AuthorizationService authorizationService;

  public Optional<User> findUserById(Integer id) {
    return userRepository.findById(id);
  }

  public <T extends EntityIdentifiedByIdOnly> T save(T entity) {
    return entityPersister.save(entity);
  }

  public <T extends EntityIdentifiedByIdOnly> T merge(T entity) {
    return entityPersister.merge(entity);
  }

  public <T extends EntityIdentifiedByIdOnly> T remove(T entity) {
    return entityPersister.remove(entity);
  }

  public Note createLink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    if (type == null || type == LinkType.NO_LINK) return null;
    Note link = buildALink(sourceNote, targetNote, creator, type, currentUTCTimestamp);
    save(link);
    return link;
  }

  public static Note buildALink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    final Note note = new Note();
    note.initialize(creator, sourceNote, currentUTCTimestamp, ":" + type.label);
    note.setTargetNote(targetNote);
    note.getRecallSetting()
        .setLevel(
            Math.max(
                sourceNote.getRecallSetting().getLevel(),
                targetNote.getRecallSetting().getLevel()));

    return note;
  }

  public Answer createAnswerForQuestion(
      AnswerableQuestionInstance answerableQuestionInstance, AnswerDTO answerDTO) {
    Answer answer = answerableQuestionInstance.buildAnswer(answerDTO);
    save(answerableQuestionInstance);
    return answer;
  }
}
