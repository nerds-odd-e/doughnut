package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.builders.*;
import org.springframework.beans.BeanUtils;
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

  public BazaarNotebookBuilder aBazaarNodebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public <T> T refresh(T object) {
    flush();
    modelFactoryService.entityManager.refresh(object);
    return object;
  }

  public void flush() {
    modelFactoryService.entityManager.flush();
  }

  public ReviewPointBuilder aReviewPointFor(Note note) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForThing(note.getThing());
    return new ReviewPointBuilder(reviewPoint, this).forNote(note);
  }

  public ReviewPointBuilder aReviewPointFor(Link link) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForThing(link.getThing());
    return new ReviewPointBuilder(reviewPoint, this).forLink(link);
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

  public UserModel aNullUserModel() {
    return modelFactoryService.toUserModel(null);
  }

  public ReviewSettingBuilder aReviewSettingFor(Note note) {
    return new ReviewSettingBuilder(this, note);
  }

  public QuizQuestionBuilder aQuestion() {
    return new QuizQuestionBuilder(this);
  }

  public QuizQuestion buildAQuestion(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    return aQuestion().buildValid(questionType, reviewPoint).ViewedByUserPlease();
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
  }

  public SuggestedQuestionForFineTuningBuilder aQuestionSuggestionForFineTunining() {
    return new SuggestedQuestionForFineTuningBuilder(this);
  }

  public NoteSimple convertToSimple(Note note) {
    if (note.getId() == null) {
      NoteSimple simpleNote = new NoteSimple();
      BeanUtils.copyProperties(note, simpleNote);
      return simpleNote;
    }
    return modelFactoryService.entityManager.find(NoteSimple.class, note.getId());
  }
}
