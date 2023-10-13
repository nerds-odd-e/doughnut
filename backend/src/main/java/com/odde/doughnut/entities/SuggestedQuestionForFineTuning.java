package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.FeedbackData;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.sql.Timestamp;
import java.util.List;
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

  @Column(name = "is_positive_feedback")
  @Getter
  @Setter
  private boolean isPositiveFeedback;

  @Column(name = "preserved_question")
  private String preservedQuestion;

  @Column(name = "created_at")
  @Getter
  @Setter
  @Nullable
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "is_duplicated")
  @Getter
  @Setter
  private boolean isDuplicated;

  @Nullable
  public int getQuizQuestionId() {
    return quizQuestion.getId();
  }

  public MCQWithAnswer getPreservedQuestion() {
    try {
      return new ObjectMapper().readValue(preservedQuestion, MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void setPreservedQuestion(MCQWithAnswer mcqWithAnswer) {
    this.preservedQuestion = mcqWithAnswer.toJsonString();
  }

  @JsonIgnore
  private Note getNote() {
    return quizQuestion.getThing().getNote();
  }

  @JsonIgnore
  public FeedbackData toFineTuningExample() {
    List<ChatMessage> messages =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(getNote())
            .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
            .addMessage(ChatMessageRole.ASSISTANT, preservedQuestion)
            .build()
            .getMessages();
    return FeedbackData.fromChatMessages(messages);
  }

  @JsonIgnore
  public FeedbackData toEvaluationData() {
    List<ChatMessage> messages =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(getNote())
            .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
            .addMessage(ChatMessageRole.ASSISTANT, preservedQuestion)
            .addFeedback(isPositiveFeedback)
            .build()
            .getMessages();
    return FeedbackData.fromChatMessages(messages);
  }
}
