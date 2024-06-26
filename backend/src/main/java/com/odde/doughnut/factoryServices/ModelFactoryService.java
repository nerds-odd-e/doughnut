package com.odde.doughnut.factoryServices;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.models.*;
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
  @Autowired public ReviewPointRepository reviewPointRepository;
  @Autowired public CircleRepository circleRepository;
  @Autowired public UserAssistantThreadRepository userAssistantThreadRepository;
  @Autowired public FailureReportRepository failureReportRepository;
  @Autowired public NotebookAssistantRepository notebookAssistantRepository;
  @Autowired public GlobalSettingRepository globalSettingRepository;
  @Autowired public EntityManager entityManager;

  @Autowired
  public QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

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

  public UserModel toUserModel(User user) {
    return new UserModel(user, this);
  }

  public ReviewPointModel toReviewPointModel(ReviewPoint reviewPoint) {
    return new ReviewPointModel(reviewPoint, this);
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

  public SearchTermModel toSearchTermModel(User entity, SearchTerm searchTerm) {
    return new SearchTermModel(entity, noteRepository, searchTerm);
  }

  public AnswerModel toAnswerModel(Answer answer) {
    return new AnswerModel(answer, this);
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

  public <T extends EntityIdentifiedByIdOnly> T remove(T entity) {
    T nb = entityManager.merge(entity);
    entityManager.remove(nb);
    entityManager.flush();
    return nb;
  }

  public LinkingNote createLink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    if (type == null || type == LinkType.NO_LINK) return null;
    LinkingNote link = buildALink(sourceNote, targetNote, creator, type, currentUTCTimestamp);
    save(link);
    return link;
  }

  public static LinkingNote buildALink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    LinkingNote note =
        LinkingNote.createLink(creator, sourceNote, currentUTCTimestamp, ":" + type.label);
    note.setTargetNote(targetNote);
    note.getReviewSetting()
        .setLevel(
            Math.max(
                sourceNote.getReviewSetting().getLevel(),
                targetNote.getReviewSetting().getLevel()));

    return note;
  }

  public void delete(QuizQuestionAndAnswer quizQuestionAndAnswer) {
    entityManager.remove(quizQuestionAndAnswer);
  }
}
