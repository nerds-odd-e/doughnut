package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfLink;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfReviewPoint;
import com.odde.doughnut.models.quizFacotries.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

  public Boolean isAnswerCorrect(Note answerNote, String spellingAnswer) {
    if (getQuestionType() == QuestionType.JUST_REVIEW
        || getQuestionType() == QuestionType.AI_QUESTION) {
      return spellingAnswer.equals("yes");
    }
    return buildPresenter().knownRightAnswers().stream()
        .anyMatch(
            correctAnswerNote -> {
              if (answerNote != null) {
                return correctAnswerNote.equals(answerNote);
              }
              return correctAnswerNote.getNoteTitle().matches(spellingAnswer);
            });
  }

  public QuizQuestionPresenter buildPresenter() {
    return getQuestionType().presenter.apply(this);
  }

  public enum QuestionType {
    JUST_REVIEW(0, null, JustReviewQuizPresenter::new),
    CLOZE_SELECTION(1, ClozeTitleSelectionQuizFactory::new, ClozeTitleSelectionQuizPresenter::new),
    SPELLING(2, SpellingQuizFactory::new, SpellingQuizPresenter::new),
    PICTURE_TITLE(
        3, PictureTitleSelectionQuizFactory::new, PictureTitleSelectionQuizPresenter::new),
    PICTURE_SELECTION(4, PictureSelectionQuizFactory::new, PictureSelectionQuizPresenter::new),
    LINK_TARGET(5, LinkTargetQuizFactory::new, LinkTargetQuizPresenter::new),
    LINK_SOURCE(6, LinkSourceQuizFactory::new, LinkSourceQuizPresenter::new),
    LINK_SOURCE_WITHIN_SAME_LINK_TYPE(
        14,
        LinkSourceWithinSameLinkTypeQuizFactory::new,
        LinkSourceWithinSameLinkTypeQuizPresenter::new),
    CLOZE_LINK_TARGET(7, ClozeLinkTargetQuizFactory::new, ClozeLinkTargetQuizPresenter::new),
    DESCRIPTION_LINK_TARGET(
        8, DescriptionLinkTargetQuizFactory::new, DescriptionLinkTargetQuizPresenter::new),
    WHICH_SPEC_HAS_INSTANCE(
        9, WhichSpecHasInstanceQuizFactory::new, WhichSpecHasInstanceQuizPresenter::new),
    FROM_SAME_PART_AS(10, FromSamePartAsQuizFactory::new, FromSamePartAsQuizPresenter::new),
    FROM_DIFFERENT_PART_AS(
        11, FromDifferentPartAsQuizFactory::new, FromDifferentPartAsQuizPresenter::new),
    AI_QUESTION(12, AiQuestionFactory::new, JustReviewQuizPresenter::new);

    public final Integer id;
    public final BiFunction<ReviewPoint, QuizQuestionServant, QuizQuestionFactory> factory;
    public final Function<QuizQuestion, QuizQuestionPresenter> presenter;

    QuestionType(
        Integer id,
        BiFunction<ReviewPoint, QuizQuestionServant, QuizQuestionFactory> factory,
        Function<QuizQuestion, QuizQuestionPresenter> presenter) {
      this.id = id;
      this.factory = factory;
      this.presenter = presenter;
    }

    private static final Map<Integer, QuestionType> idMap =
        Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(x -> x.id, x -> x)));

    public static QuestionType fromId(Integer id) {
      return idMap.getOrDefault(id, null);
    }
  }

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonUseIdInsteadOfReviewPoint
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "review_point_id", referencedColumnName = "id")
  @Getter
  @Setter
  private ReviewPoint reviewPoint;

  @Column(name = "question_type")
  @Getter
  @Setter
  private Integer questionTypeId;

  @Transient @Getter @Setter private String rawJsonQuestion;

  @JsonUseIdInsteadOfLink
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Link categoryLink;

  @Column(name = "option_thing_ids")
  @Getter
  @Setter
  private String optionThingIds = "";

  @Column(name = "vice_review_point_ids")
  @Getter
  private String viceReviewPointIds = "";

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public void setViceReviewPoints(List<ReviewPoint> reviewPoints) {
    if (reviewPoints == null) {
      viceReviewPointIds = "";
      return;
    }
    viceReviewPointIds =
        reviewPoints.stream()
            .map(ReviewPoint::getId)
            .map(Object::toString)
            .collect(Collectors.joining(","));
  }

  public void setQuestionType(QuestionType questionType) {
    this.questionTypeId = questionType.id;
  }

  @JsonIgnore
  public QuestionType getQuestionType() {
    return QuestionType.fromId(questionTypeId);
  }

  @JsonIgnore
  public List<Integer> getViceReviewPointIdList() {
    if (Strings.isBlank(viceReviewPointIds)) return List.of();
    return Arrays.stream(viceReviewPointIds.split(","))
        .map(Integer::valueOf)
        .collect(Collectors.toList());
  }
}
