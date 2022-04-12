package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfLink;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfReviewPoint;
import com.odde.doughnut.models.quizFacotries.ClozeLinkTargetQuizFactory;
import com.odde.doughnut.models.quizFacotries.ClozeLinkTargetQuizPresenter;
import com.odde.doughnut.models.quizFacotries.ClozeTitleSelectionQuizFactory;
import com.odde.doughnut.models.quizFacotries.ClozeTitleSelectionQuizPresenter;
import com.odde.doughnut.models.quizFacotries.DescriptionLinkTargetQuizFactory;
import com.odde.doughnut.models.quizFacotries.DescriptionLinkTargetQuizPresenter;
import com.odde.doughnut.models.quizFacotries.FromDifferentPartAsQuizFactory;
import com.odde.doughnut.models.quizFacotries.FromDifferentPartAsQuizPresenter;
import com.odde.doughnut.models.quizFacotries.FromSamePartAsQuizFactory;
import com.odde.doughnut.models.quizFacotries.FromSamePartAsQuizPresenter;
import com.odde.doughnut.models.quizFacotries.JustReviewQuizFactory;
import com.odde.doughnut.models.quizFacotries.JustReviewQuizPresenter;
import com.odde.doughnut.models.quizFacotries.LinkSourceExclusiveQuizFactory;
import com.odde.doughnut.models.quizFacotries.LinkSourceExclusiveQuizPresenter;
import com.odde.doughnut.models.quizFacotries.LinkSourceQuizFactory;
import com.odde.doughnut.models.quizFacotries.LinkSourceQuizPresenter;
import com.odde.doughnut.models.quizFacotries.LinkTargetQuizFactory;
import com.odde.doughnut.models.quizFacotries.LinkTargetQuizPresenter;
import com.odde.doughnut.models.quizFacotries.PictureSelectionQuizFactory;
import com.odde.doughnut.models.quizFacotries.PictureSelectionQuizPresenter;
import com.odde.doughnut.models.quizFacotries.PictureTitleSelectionQuizFactory;
import com.odde.doughnut.models.quizFacotries.PictureTitleSelectionQuizPresenter;
import com.odde.doughnut.models.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.models.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.models.quizFacotries.SpellingQuizFactory;
import com.odde.doughnut.models.quizFacotries.SpellingQuizPresenter;
import com.odde.doughnut.models.quizFacotries.WhichSpecHasInstanceQuizFactory;
import com.odde.doughnut.models.quizFacotries.WhichSpecHasInstanceQuizPresenter;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.apache.logging.log4j.util.Strings;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

  public enum QuestionType {
    CLOZE_SELECTION(1, ClozeTitleSelectionQuizFactory::new, ClozeTitleSelectionQuizPresenter::new),
    SPELLING(2, SpellingQuizFactory::new, SpellingQuizPresenter::new),
    PICTURE_TITLE(
        3, PictureTitleSelectionQuizFactory::new, PictureTitleSelectionQuizPresenter::new),
    PICTURE_SELECTION(4, PictureSelectionQuizFactory::new, PictureSelectionQuizPresenter::new),
    LINK_TARGET(5, LinkTargetQuizFactory::new, LinkTargetQuizPresenter::new),
    LINK_SOURCE(6, LinkSourceQuizFactory::new, LinkSourceQuizPresenter::new),
    CLOZE_LINK_TARGET(7, ClozeLinkTargetQuizFactory::new, ClozeLinkTargetQuizPresenter::new),
    DESCRIPTION_LINK_TARGET(
        8, DescriptionLinkTargetQuizFactory::new, DescriptionLinkTargetQuizPresenter::new),
    WHICH_SPEC_HAS_INSTANCE(
        9, WhichSpecHasInstanceQuizFactory::new, WhichSpecHasInstanceQuizPresenter::new),
    FROM_SAME_PART_AS(10, FromSamePartAsQuizFactory::new, FromSamePartAsQuizPresenter::new),
    FROM_DIFFERENT_PART_AS(
        11, FromDifferentPartAsQuizFactory::new, FromDifferentPartAsQuizPresenter::new),
    LINK_SOURCE_EXCLUSIVE(
        12, LinkSourceExclusiveQuizFactory::new, LinkSourceExclusiveQuizPresenter::new),
    JUST_REVIEW(13, JustReviewQuizFactory::new, JustReviewQuizPresenter::new);

    public final Integer id;
    public final Function<ReviewPoint, QuizQuestionFactory> factory;
    public final Function<QuizQuestion, QuizQuestionPresenter> presenter;

    QuestionType(
        Integer id,
        Function<ReviewPoint, QuizQuestionFactory> factory,
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

  @JsonUseIdInsteadOfLink
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Link categoryLink;

  @Column(name = "option_notes")
  @Getter
  @Setter
  private String optionNoteIds = "";

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

  @JsonIgnore
  public void setOptionNotes(List<Note> notes) {
    optionNoteIds =
        notes.stream().map(Note::getId).map(Object::toString).collect(Collectors.joining(","));
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
