package com.odde.doughnut.testability;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.LinkQuestionType;
import com.odde.doughnut.testability.builders.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MakeMe extends MakeMeWithoutDB {
  @Autowired public ModelFactoryService modelFactoryService;

  private MakeMe() {}

  public static MakeMe makeMeWithoutFactoryService() {
    return new MakeMe();
  }

  public UserBuilder aUser() {
    return new UserBuilder(this);
  }

  public UserBuilder anAdmin() {
    return new UserBuilder(this, "admin");
  }

  public NoteBuilder aNote() {
    return new NoteBuilder(this);
  }

  public NoteBuilder aNote(String title) {
    return aNote().titleConstructor(title);
  }

  public NoteBuilder aHeadNote(String title) {
    return aNote().titleConstructor(title);
  }

  public NoteBuilder aNote(String title, String details) {
    return aNote().titleConstructor(title).details(details);
  }

  public NoteBuilder theNote(Note note) {
    return new NoteBuilder(note, this);
  }

  public BazaarNotebookBuilder aBazaarNotebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public AssessmentAttemptHistoryBuilder aAssessmentAttemptHistory(
      AssessmentAttemptHistory assessmentAttemptHistory) {
    return new AssessmentAttemptHistoryBuilder(this, assessmentAttemptHistory);
  }

  public <T> T refresh(T object) {
    modelFactoryService.entityManager.flush();
    modelFactoryService.entityManager.refresh(object);
    return object;
  }

  public ReviewPointBuilder aReviewPointFor(Note note) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForNote(note);
    ReviewPointBuilder reviewPointBuilder = new ReviewPointBuilder(reviewPoint, this);
    reviewPointBuilder.entity.setNote(note);
    return reviewPointBuilder;
  }

  public ReviewPointBuilder aReviewPointBy(UserModel user) {
    Note note = aNote().please();
    return aReviewPointFor(note).by(user);
  }

  public CircleBuilder aCircle() {
    return new CircleBuilder(null, this);
  }

  public CircleBuilder theCircle(CircleModel circleModel) {
    return new CircleBuilder(circleModel, this);
  }

  public ImageBuilder anImage() {
    return new ImageBuilder(new Image(), this);
  }

  public AnswerViewedByUserBuilder anAnswerViewedByUser() {
    return new AnswerViewedByUserBuilder(this);
  }

  public AnswerBuilder anAnswer() {
    return new AnswerBuilder(this);
  }

  public SubscriptionBuilder aSubscription() {
    return new SubscriptionBuilder(this, new Subscription());
  }

  public LinkBuilder aLink() {
    return new LinkBuilder(this);
  }

  public UserModel aNullUserModelPlease() {
    return modelFactoryService.toUserModel(null);
  }

  public QuizQuestionBuilder aQuestion() {
    return new QuizQuestionBuilder(this);
  }

  public QuizQuestionAndAnswer buildAQuestionForLinkingNote(
      LinkQuestionType linkQuestionType, LinkingNote linkingNote, User user) {
    QuizQuestionServant servant =
        new QuizQuestionServant(user, new NonRandomizer(), modelFactoryService);
    try {
      return linkQuestionType
          .factoryForLinkingNote
          .apply(linkingNote, servant)
          .buildValidQuizQuestion();
    } catch (QuizQuestionNotPossibleException e) {
      return null;
    }
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
  }

  public SuggestedQuestionForFineTuningBuilder aQuestionSuggestionForFineTunining() {
    return new SuggestedQuestionForFineTuningBuilder(this);
  }

  public AudioBuilder anAudio() {
    return new AudioBuilder(new Audio(), this);
  }

  public UserAssistantThreadBuilder aUserAssistantThread(String threadId) {
    return new UserAssistantThreadBuilder(this, threadId);
  }
}
