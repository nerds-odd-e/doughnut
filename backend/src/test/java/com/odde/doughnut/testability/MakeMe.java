package com.odde.doughnut.testability;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.builders.AnswerBuilder;
import com.odde.doughnut.testability.builders.BazaarNotebookBuilder;
import com.odde.doughnut.testability.builders.CircleBuilder;
import com.odde.doughnut.testability.builders.CommentBuilder;
import com.odde.doughnut.testability.builders.FakeBindingResult;
import com.odde.doughnut.testability.builders.ImageBuilder;
import com.odde.doughnut.testability.builders.LinkBuilder;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.QuizQuestionBuilder;
import com.odde.doughnut.testability.builders.ReviewPointBuilder;
import com.odde.doughnut.testability.builders.ReviewSettingBuilder;
import com.odde.doughnut.testability.builders.SubscriptionBuilder;
import com.odde.doughnut.testability.builders.TimestampBuilder;
import com.odde.doughnut.testability.builders.UploadedPictureBuilder;
import com.odde.doughnut.testability.builders.UserBuilder;
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
    return new NoteBuilder(new Note(), this);
  }

  public NoteBuilder aNote(String title) {
    return aNote().title(title);
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
    ReviewPoint reviewPoint = new ReviewPoint();
    return new ReviewPointBuilder(reviewPoint, this).forNote(note);
  }

  public ReviewPointBuilder aReviewPointFor(Link link) {
    ReviewPoint reviewPoint = new ReviewPoint();
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

  public CommentBuilder aComment() {
    return new CommentBuilder(this);
  }
}
