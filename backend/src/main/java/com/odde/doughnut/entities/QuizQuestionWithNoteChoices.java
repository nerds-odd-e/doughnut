package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Entity
public abstract class QuizQuestionWithNoteChoices extends QuizQuestionEntity {

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

  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    List<Integer> idList = getChoiceNoteIds();
    return modelFactoryService
        .noteRepository
        .findAllByIds(idList)
        .sorted(Comparator.comparing(v -> idList.indexOf(v.getId())))
        .map(this::noteToChoice)
        .toList();
  }

  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    return choice;
  }

  public Optional<ImageWithMask> getImageWithMask() {
    return Optional.empty();
  }

  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }
}
