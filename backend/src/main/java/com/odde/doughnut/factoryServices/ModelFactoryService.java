package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.NotebookService;
import jakarta.persistence.EntityManager;

import java.math.RoundingMode;
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
  @Autowired public NotebookAssistantRepository notebookAssistantRepository;
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
  @Autowired public PlayersRepository playersRepository;
  @Autowired public GamesRepository gamesRepository;
  @Autowired public RoundsRepository roundsRepository;
  

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

  @Autowired public NotebookAiAssistantRepository notebookAiAssistantRepository;

  @Autowired public NoteEmbeddingRepository noteEmbeddingRepository;
  @Autowired public NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;

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

  public NoteModel toNoteModel(Note note) {
    return new NoteModel(note, this);
  }

  public NoteMotionModel motionOfMoveAfter(Note subject, Note target, Boolean asFirstChild) {
    return new NoteMotionModel(subject, target, asFirstChild, this);
  }

  public NoteMotionModel motionOfMoveUnder(Note sourceNote, Note targetNote, Boolean asFirstChild) {
    if (!asFirstChild) {
      List<Note> children = targetNote.getChildren();
      if (!children.isEmpty()) {
        return motionOfMoveAfter(sourceNote, children.getLast(), false);
      }
    }
    return motionOfMoveAfter(sourceNote, targetNote, true);
  }

  public BazaarModel toBazaarModel() {
    return new BazaarModel(this);
  }

  public Optional<User> findUserById(Integer id) {
    return userRepository.findById(id);
  }

  public Optional<User> findUserByToken(String token) {
    UserToken usertoken = userTokenRepository.findByToken(token);
    return this.findUserById(usertoken.getUserId());
  }

  public UserModel toUserModel(User user) {
    return new UserModel(user, this);
  }

  public CircleModel toCircleModel(Circle circle) {
    return new CircleModel(circle, this);
  }

  public CircleModel findCircleByInvitationCode(String invitationCode) {
    Circle circle = circleRepository.findFirstByInvitationCode(invitationCode);
    if (circle == null) {
      return null;
    }
    return toCircleModel(circle);
  }

  public SubscriptionModel toSubscriptionModel(Subscription sub) {
    return new SubscriptionModel(sub, this);
  }

  public Authorization toAuthorization(User entity) {
    return new Authorization(entity, this);
  }

  public SuggestedQuestionForFineTuningModel toSuggestedQuestionForFineTuningService(
      SuggestedQuestionForFineTuning suggestion) {
    return new SuggestedQuestionForFineTuningModel(suggestion, this);
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

  public NotebookService notebookService(Notebook notebook) {
    return new NotebookService(notebook, this);
  }

  public Answer createAnswerForQuestion(
      AnswerableQuestionInstance answerableQuestionInstance, AnswerDTO answerDTO) {
    Answer answer = answerableQuestionInstance.buildAnswer(answerDTO);
    save(answerableQuestionInstance);
    return answer;
  }
}
