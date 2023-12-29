package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import java.sql.Timestamp;
import java.util.Arrays;
import javax.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
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

  @Column(name = "preserved_note_content")
  @Getter
  @Setter
  private String preservedNoteContent;

  @Column(name = "real_correct_answers")
  @Getter
  @Setter
  private String realCorrectAnswers = "";

  @Column(name = "created_at")
  @Getter
  @Setter
  @Nullable
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public MCQWithAnswer getPreservedQuestion() {
    try {
      return new ObjectMapper().readValue(preservedQuestion, MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void preserveQuestion(MCQWithAnswer mcqWithAnswer) {
    this.preservedQuestion = mcqWithAnswer.toJsonString();
  }

  public void preserveNoteContent(Note note) {
    this.preservedNoteContent = note.getNoteDescription();
  }

  @JsonIgnore
  public OpenAIChatGPTFineTuningExample toQuestionGenerationFineTuningExample() {
    AiToolList tool = AiToolFactory.mcqWithAnswerAiTool();
    return getOpenAIChatGPTFineTuningExample(tool, getPreservedQuestion());
  }

  @JsonIgnore
  public OpenAIChatGPTFineTuningExample toQuestionEvaluationFineTuningData() {
    AiToolList tool = AiToolFactory.questionEvaluationAiTool(getPreservedQuestion());
    return getOpenAIChatGPTFineTuningExample(tool, getQuestionEvaluation());
  }

  private OpenAIChatGPTFineTuningExample getOpenAIChatGPTFineTuningExample(
      AiToolList tool, Object preservedQuestion1) {
    OpenAIChatRequestBuilder openAIChatRequestBuilder = new OpenAIChatRequestBuilder();
    openAIChatRequestBuilder.addSystemMessage(preservedNoteContent);
    tool.addToChat(openAIChatRequestBuilder);
    openAIChatRequestBuilder.addFunctionCallMessage(
        preservedQuestion1, tool.getFirstFunctionName());
    var messages = openAIChatRequestBuilder.buildMessages();
    return OpenAIChatGPTFineTuningExample.from(messages);
  }

  @JsonIgnore
  private QuestionEvaluation getQuestionEvaluation() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.comment = comment;
    questionEvaluation.feasibleQuestion = isPositiveFeedback;
    questionEvaluation.correctChoices = getRealCorrectAnswerIndice();
    return questionEvaluation;
  }

  @JsonIgnore
  private int[] getRealCorrectAnswerIndice() {
    if (isPositiveFeedback) {
      return new int[] {getPreservedQuestion().correctChoiceIndex};
    }
    if (!Strings.isBlank(realCorrectAnswers)) {
      return Arrays.stream(realCorrectAnswers.split(","))
          .map(String::trim)
          .mapToInt(Integer::parseInt)
          .toArray();
    }
    return null;
  }
}
