package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.models.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_point")
public class ReviewPoint {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  public static ReviewPoint buildReviewPointForThing(Thing thing) {
    ReviewPoint entity = new ReviewPoint();
    entity.setThing(thing);
    return entity;
  }

  public QuizQuestion createAQuizQuestionOfType(QuestionType questionType) {
    QuizQuestion quizQuestion = new QuizQuestion();
    quizQuestion.setReviewPoint(this);
    quizQuestion.setQuestionType(questionType);
    return quizQuestion;
  }

  @Override
  public String toString() {
    return "ReviewPoint{" + "id=" + id + '}';
  }

  @ManyToOne
  @JoinColumn(name = "thing_id")
  @Getter
  @Setter
  private Thing thing;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;

  @Column(name = "last_reviewed_at")
  @Getter
  @Setter
  private Timestamp lastReviewedAt;

  @Column(name = "next_review_at")
  @Getter
  @Setter
  private Timestamp nextReviewAt;

  @Column(name = "initial_reviewed_at")
  @Getter
  @Setter
  private Timestamp initialReviewedAt;

  @Column(name = "repetition_count")
  @Getter
  @Setter
  private Integer repetitionCount = 0;

  @Column(name = "forgetting_curve_index")
  @Getter
  @Setter
  private Integer forgettingCurveIndex = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;

  @Column(name = "removed_from_review")
  @Getter
  @Setter
  private Boolean removedFromReview = false;

  private ReviewPoint() {}

  @JsonIgnore
  public Note getNote() {
    return this.thing.getNote();
  }

  @JsonIgnore
  public Link getLink() {
    return this.thing.getLink();
  }

  public boolean isInitialReviewOnSameDay(Timestamp currentTime, ZoneId timeZone) {
    return TimestampOperations.getDayId(getInitialReviewedAt(), timeZone)
        == TimestampOperations.getDayId(currentTime, timeZone);
  }

  public List<QuizQuestion.QuestionType> availableQuestionTypes(
      Boolean aiQuestionTypeOnlyForReview) {
    List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
    if (getLink() != null) {
      Collections.addAll(questionTypes, getLink().getLinkType().getQuestionTypes());
    } else {
      if (aiQuestionTypeOnlyForReview) {
        questionTypes.add(QuestionType.AI_QUESTION);
      } else {
        questionTypes.add(QuestionType.SPELLING);
        questionTypes.add(QuestionType.CLOZE_SELECTION);
        questionTypes.add(QuestionType.PICTURE_TITLE);
        questionTypes.add(QuestionType.PICTURE_SELECTION);
      }
    }
    return questionTypes;
  }

  @JsonIgnore
  public Note getHeadNote() {
    return this.thing.getHeadNoteOfNotebook();
  }

  public Timestamp calculateNextReviewAt() {
    return TimestampOperations.addHoursToTimestamp(
        getLastReviewedAt(), forgettingCurve().getRepeatInHours());
  }

  private ForgettingCurve forgettingCurve() {
    return new ForgettingCurve(getUser().getSpacedRepetitionAlgorithm(), getForgettingCurveIndex());
  }

  public void reviewFailed(Timestamp currentUTCTimestamp) {
    setForgettingCurveIndex(forgettingCurve().failed());
    setNextReviewAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));
  }

  public void reviewedSuccessfully(Timestamp currentUTCTimestamp) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextReviewAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours));

    setLastReviewedAt(currentUTCTimestamp);
    setNextReviewAt(calculateNextReviewAt());
  }

  @JsonIgnore
  public boolean isDescriptionBlankHtml() {
    return getNote().isDescriptionBlankHtml();
  }
}
