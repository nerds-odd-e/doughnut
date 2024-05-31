package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public abstract class QuizQuestionWithNoteChoices extends QuizQuestionEntity {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id")
  @Getter
  @Setter
  private LinkingNote categoryLink;

  public void setChoicesAndRightAnswer(
      Note answerNote, List<? extends Note> options, Randomizer randomizer) {
    List<Note> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Note> shuffled = randomizer.shuffle(optionsEntities);
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.stem = getStem();
    mcqWithAnswer.correctChoiceIndex = shuffled.indexOf(answerNote);
    mcqWithAnswer.choices =
        shuffled.stream().map(this::noteToChoice).map(QuizQuestion.Choice::getDisplay).toList();
    setMcqWithAnswer(mcqWithAnswer);
  }

  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    MCQWithAnswer mcqWithAnswer = getMcqWithAnswer();
    return mcqWithAnswer.choices.stream()
        .map(
            s -> {
              QuizQuestion.Choice choice = new QuizQuestion.Choice();
              choice.setDisplay(s);
              return choice;
            })
        .toList();
  }

  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    return choice;
  }
}
