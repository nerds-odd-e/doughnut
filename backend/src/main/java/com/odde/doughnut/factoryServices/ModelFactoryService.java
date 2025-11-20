package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.services.AuthorizationService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
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
  @Autowired public EntityManager entityManager;
  @Autowired public NotebookRepository notebookRepository;
  @Autowired public CertificateRepository certificateRepository;
  @Autowired public ConversationRepository conversationRepository;
  @Autowired public ConversationMessageRepository conversationMessageRepository;
  @Autowired public NotebookCertificateApprovalRepository notebookCertificateApprovalRepository;
  @Autowired public RecallPromptRepository recallPromptRepository;
  @Autowired public UserTokenRepository userTokenRepository;

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

  @Autowired public NotebookAiAssistantRepository notebookAiAssistantRepository;

  @Autowired public NoteEmbeddingRepository noteEmbeddingRepository;
  @Autowired public NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;

  @Autowired public AuthorizationService authorizationService;

  public void storeNoteEmbedding(Note note, java.util.List<Float> embedding) {
    noteEmbeddingJdbcRepository.insert(note.getId(), embedding);
  }

  public void deleteNoteEmbeddingByNoteId(Integer noteId) {
    noteEmbeddingRepository.deleteByNoteId(noteId);
  }

  public void deleteNoteEmbeddingsByNotebookId(Integer notebookId) {
    noteEmbeddingRepository.deleteByNotebookId(notebookId);
  }

  public java.util.Optional<java.util.List<Float>> getNoteEmbeddingAsFloats(Integer noteId) {
    return noteEmbeddingJdbcRepository
        .select(noteId)
        .map(
            bytes -> {
              NoteEmbedding ne = new NoteEmbedding();
              ne.setEmbedding(bytes);
              return ne.getEmbeddingAsFloats();
            });
  }

  public Optional<User> findUserById(Integer id) {
    return userRepository.findById(id);
  }

  public Optional<User> findUserByToken(String token) {
    UserToken usertoken = userTokenRepository.findByToken(token);

    if (usertoken == null) {
      AuthorizationService.throwUserNotFound();
    }

    return this.findUserById(usertoken.getUserId());
  }

  public Optional<List<UserToken>> findTokensByUser(Integer id) {
    List<UserToken> usertokens = userTokenRepository.findByUserId(id);
    return Optional.ofNullable(usertokens);
  }

  public Optional<UserToken> findTokenByTokenId(Integer id) {
    return userTokenRepository.findById(id);
  }

  public void deleteToken(Integer tokenId) {
    userTokenRepository.deleteById(tokenId);
  }

  public <T extends EntityIdentifiedByIdOnly> T save(T entity) {
    if (entity.getId() == null) {
      entityManager.persist(entity);
      return entity;
    }
    return entityManager.merge(entity);
  }

  public <T extends EntityIdentifiedByIdOnly> T merge(T entity) {
    return entityManager.merge(entity);
  }

  public <T extends EntityIdentifiedByIdOnly> T remove(T entity) {
    T nb = entityManager.merge(entity);
    entityManager.remove(nb);
    entityManager.flush();
    return nb;
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
