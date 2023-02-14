package com.odde.doughnut.testability;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.builders.*;
import java.nio.CharBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class MakeMe {
  @Autowired public ModelFactoryService modelFactoryService;

  public UserBuilder aUser() {
    return new UserBuilder(this);
  }

  public UserBuilder aDeveloper() {
    return new UserBuilder(this, "Developer");
  }

  public NoteBuilder aNote() {
    return new NoteBuilder(this);
  }

  public NoteBuilder aNote(String title) {
    return aNote().title(title);
  }

  public NoteBuilder aHeadNote() {
    return aNote().asHeadNoteOfANotebook();
  }

  public NoteBuilder aHeadNote(String title) {
    return aHeadNote().title(title);
  }

  public NoteBuilder aNote(String title, String description) {
    return aNote().title(title).description(description);
  }

  public NoteBuilder theNote(Note note) {
    return new NoteBuilder(note, this);
  }

  public BazaarNotebookBuilder aBazaarNodebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public <T> T refresh(T object) {
    modelFactoryService.entityManager.refresh(object);
    return object;
  }

  public String aStringOfLength(int length, char withChar) {
    return CharBuffer.allocate(length).toString().replace('\0', withChar);
  }

  public String aStringOfLength(int length) {
    return aStringOfLength(length, 'a');
  }

  public BindingResult successfulBindingResult() {
    return new FakeBindingResult(false);
  }

  public BindingResult failedBindingResult() {
    return new FakeBindingResult(true);
  }

  public ReviewPointBuilder aReviewPointFor(Note note) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForThing(note.getThing());
    return new ReviewPointBuilder(reviewPoint, this).forNote(note);
  }

  public ReviewPointBuilder aReviewPointFor(Link link) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForThing(link.getThing());
    return new ReviewPointBuilder(reviewPoint, this).forLink(link);
  }

  public TimestampBuilder aTimestamp() {
    return new TimestampBuilder();
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

  public UploadedPictureBuilder anUploadedPicture() {
    return new UploadedPictureBuilder();
  }

  public AnswerViewedByUserBuilder anAnswerViewedByUserFor(ReviewPoint reviewPoint) {
    return new AnswerViewedByUserBuilder(this).forReviewPoint(reviewPoint);
  }

  public AnswerBuilder anAnswerFor(ReviewPoint reviewPoint) {
    return new AnswerBuilder(this).forReviewPoint(reviewPoint);
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

  public QuizQuestionViewedByUser buildAQuestion(
      QuizQuestion.QuestionType questionType, ReviewPoint reviewPoint) {
    return aQuestion().buildValid(questionType, reviewPoint).ViewedByUserPlease();
  }

  public WikidataEntityJsonBuilder wikidataEntityJson() {
    return new WikidataEntityJsonBuilder();
  }

  public WikidataClaimJsonBuilder wikidataClaimsJson(String wikidataId) {
    return new WikidataClaimJsonBuilder(wikidataId);
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
  }

  public OpenAICompletionResultBuilder openAiCompletionResult() {
    return new OpenAICompletionResultBuilder();
  }
}
