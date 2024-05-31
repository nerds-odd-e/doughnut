package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type", discriminatorType = DiscriminatorType.INTEGER)
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "imageWithMask"})
public abstract class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "raw_json_question")
  @Getter
  @Setter
  private String rawJsonQuestion;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    try {
      return new ObjectMapper().readValue(getRawJsonQuestion(), MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public String getStem() {
    return getMcqWithAnswer().stem;
  }

  public Integer getCorrectAnswerIndex() {
    return getMcqWithAnswer().correctChoiceIndex;
  }

  public String getMainTopic() {
    return null;
  }

  public ImageWithMask getImageWithMask() {
    return null;
  }

  public abstract List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService);
}
