package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.SearchTerm;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.models.NoteMotionModel;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.SubscriptionModel;
import com.odde.doughnut.models.UserModel;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
  @Autowired public TextContentRepository textContentRepository;
  @Autowired public ThingRepository thingRepository;
  @Autowired public NoteRepository noteRepository;
  @Autowired public UserRepository userRepository;
  @Autowired public BazaarNotebookRepository bazaarNotebookRepository;
  @Autowired public ReviewPointRepository reviewPointRepository;
  @Autowired public CircleRepository circleRepository;
  @Autowired public LinkRepository linkRepository;
  @Autowired public QuizQuestionRepository quizQuestionRepository;
  @Autowired public AnswerRepository answerRepository;
  @Autowired public NotesClosureRepository notesClosureRepository;
  @Autowired public NotebookRepository notebookRepository;
  @Autowired public EntityManager entityManager;
  @Autowired public FailureReportRepository failureReportRepository;
  @Autowired public CommentRepository commentRepository;

  public NoteModel toNoteModel(Note note) {
    return new NoteModel(note, this);
  }

  public NoteMotionModel toNoteMotionModel(NoteMotion noteMotion, Note note) {
    noteMotion.setSubject(note);
    return new NoteMotionModel(noteMotion, this);
  }

  public NoteMotionModel toNoteMotionModel(Note sourceNote, Note targetNote, Boolean asFirstChild) {
    if (!asFirstChild) {
      List<Note> children = targetNote.getChildren();
      if (children.size() > 0) {
        return toNoteMotionModel(
            new NoteMotion(children.get(children.size() - 1), false), sourceNote);
      }
    }
    return toNoteMotionModel(new NoteMotion(targetNote, true), sourceNote);
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

  public ReviewPointModel toReviewPointModel(@Valid ReviewPoint reviewPoint) {
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

  public LinkModel toLinkModel(Link link) {
    return new LinkModel(link, this);
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

  public Optional<Comment> findCommentById(Integer id) {
    return commentRepository.findById(id);
  }
}
