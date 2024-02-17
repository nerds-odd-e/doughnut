package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.factoryServices.quizFacotries.*;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Entity
@Table(name = "quiz_question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type", discriminatorType = DiscriminatorType.INTEGER)
public abstract class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id")
  @Getter
  @Setter
  private LinkingNote categoryLink;

  @Column(name = "option_thing_ids")
  @Getter
  @Setter
  private String optionNoteIds = "";

  @Column(name = "correct_answer_index")
  @Getter
  @Setter
  private Integer correctAnswerIndex;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public abstract QuizQuestionPresenter buildPresenter();

  public void setChoicesAndRightAnswer(
      Note answerNote, List<? extends Note> options, Randomizer randomizer) {
    List<Note> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Note> shuffled = randomizer.shuffle(optionsEntities);
    setCorrectAnswerIndex(shuffled.indexOf(answerNote));
    setOptionNoteIds(
        shuffled.stream().map(Note::getId).map(Object::toString).collect(Collectors.joining(",")));
  }

  @JsonIgnore
  public List<Integer> getChoiceNoteIds() {
    if (Strings.isBlank(optionNoteIds)) return List.of();
    return Arrays.stream(optionNoteIds.split(","))
        .map(Integer::parseInt)
        .collect(Collectors.toList());
  }

  @JsonIgnore
  public ReviewPoint getReviewPointFor(UserModel userModel) {
    return userModel.getReviewPointFor(getNote());
  }

  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public Integer getCorrectAnswerIndex1() {
    return getCorrectAnswerIndex();
  }
}
