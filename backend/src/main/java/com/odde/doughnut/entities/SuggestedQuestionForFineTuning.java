package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.json.TrainingData;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import java.sql.Timestamp;
import javax.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "suggested_question_for_fine_tuning")
public class SuggestedQuestionForFineTuning {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @Getter
  @Setter
  @NonNull
  @JsonIgnore
  private User user;

  @ManyToOne
  @JoinColumn(name = "quiz_question_id")
  @Getter
  @Setter
  @JsonIgnore
  @NonNull
  private QuizQuestionEntity quizQuestion;

  @Column(name = "comment")
  @Getter
  @Setter
  private String comment;

  @Column(name = "preserved_question")
  @Getter
  @Setter
  private String preservedQuestion;

  @Column(name = "created_at")
  @Getter
  @Setter
  @Nullable
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  private Note getNote() {
    return quizQuestion.getThing().getNote();
  }

  @JsonIgnore
  public TrainingData getTrainingData() {
    var chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(getNote())
            .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
            .build();
    return TrainingData.generateTrainingData(chatRequest.getMessages(), preservedQuestion);
  }
}
