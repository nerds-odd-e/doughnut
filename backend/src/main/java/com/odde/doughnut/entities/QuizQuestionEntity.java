package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.*;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type", discriminatorType = DiscriminatorType.INTEGER)
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "pictureWithMask"})
public abstract class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public abstract Integer getCorrectAnswerIndex();

  public abstract boolean checkAnswer(Answer answer);

  public abstract List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService);

  public abstract String getStem();

  public abstract String getMainTopic();

  public abstract Optional<PictureWithMask> getPictureWithMask();
}
