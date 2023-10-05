package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.NotePositionViewedByUser;
import com.odde.doughnut.factoryServices.quizFacotries.*;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.*;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

//
// The class name has Entity suffix so that it can be distinguished from the QuizQuestion class,
// which is used in the frontend from the data consumer's perspective.
//
@Entity
@Table(name = "quiz_question")
public class QuizQuestionEntity {

  public enum QuestionType {
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
    AI_QUESTION(12, AiQuestionFactory::new, AiQuestionPresenter::new);

    public final Integer id;
    public final BiFunction<Thing, QuizQuestionServant, QuizQuestionFactory> factory;
    public final Function<QuizQuestionEntity, QuizQuestionPresenter> presenter;

    QuestionType(
        Integer id,
        BiFunction<Thing, QuizQuestionServant, QuizQuestionFactory> factory,
        Function<QuizQuestionEntity, QuizQuestionPresenter> presenter) {
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

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "thing_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Thing thing;

  @Column(name = "question_type")
  @Getter
  @Setter
  private Integer questionTypeId;

  @Column(name = "raw_json_question")
  @Getter
  @Setter
  private String rawJsonQuestion;

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Link categoryLink;

  @Column(name = "option_thing_ids")
  @Getter
  @Setter
  private String optionThingIds = "";

  @Column(name = "correct_answer_index")
  @Getter
  @Setter
  private Integer correctAnswerIndex;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public void setQuestionType(QuestionType questionType) {
    this.questionTypeId = questionType.id;
  }

  @JsonIgnore
  public QuestionType getQuestionType() {
    return QuestionType.fromId(questionTypeId);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return getQuestionType().presenter.apply(this);
  }

  public void setChoicesAndRightAnswer(
      Thingy answerNote, List<? extends Thingy> options, Randomizer randomizer) {
    List<Thingy> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Thingy> shuffled = randomizer.shuffle(optionsEntities);
    setCorrectAnswerIndex(shuffled.indexOf(answerNote));
    setOptionThingIds(
        shuffled.stream()
            .map(Thingy::getThing)
            .map(Thing::getId)
            .map(Object::toString)
            .collect(Collectors.joining(",")));
  }

  @JsonIgnore
  public List<Integer> getChoiceThingIds() {
    String optionThingIds = getOptionThingIds();
    if (Strings.isBlank(optionThingIds)) return List.of();
    return Arrays.stream(optionThingIds.split(","))
        .map(Integer::parseInt)
        .collect(Collectors.toList());
  }

  @JsonIgnore
  public ReviewPoint getReviewPointFor(UserModel userModel) {
    return userModel.getReviewPointFor(thing);
  }

  @JsonIgnore
  public NotePositionViewedByUser getNotebookPosition(User user) {
    if (thing == null) {
      return null;
    }
    return new NoteViewer(user, thing.getHeadNoteOfNotebook()).jsonNotePosition(true);
  }

  public MCQWithAnswer getMcqWithAnswer() {
    try {
      return new ObjectMapper().readValue(getRawJsonQuestion(), MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
