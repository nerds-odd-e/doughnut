package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Entity
@Table(name = "suggested_question_for_fine_tuning")
public class SuggestedQuestionForFineTuning extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "user_id")
  @Getter
  @Setter
  @NotNull
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
  @NotNull
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
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public MCQWithAnswer getPreservedQuestion() {
    try {
      return new ObjectMapperConfig()
          .objectMapper()
          .readValue(preservedQuestion, MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void preserveQuestion(MCQWithAnswer mcqWithAnswer) {
    this.preservedQuestion =
        new ObjectMapperConfig().objectMapper().valueToTree(mcqWithAnswer).toString();
  }

  public void preserveNoteContent(Note note) {
    this.preservedNoteContent = note.getNoteDescription();
  }

  @JsonIgnore
  public OpenAIChatGPTFineTuningExample toQuestionGenerationFineTuningExample() {
    InstructionAndSchema tool = AiToolFactory.mcqWithAnswerAiTool();
    return getOpenAIChatGPTFineTuningExample(tool, getPreservedQuestion());
  }

  @JsonIgnore
  public OpenAIChatGPTFineTuningExample toQuestionEvaluationFineTuningData() {
    InstructionAndSchema tool = AiToolFactory.questionEvaluationAiTool(getPreservedQuestion());
    return getOpenAIChatGPTFineTuningExample(tool, getQuestionEvaluation());
  }

  private OpenAIChatGPTFineTuningExample getOpenAIChatGPTFineTuningExample(
      InstructionAndSchema tool, Object argument) {
    OpenAIChatRequestBuilder builder =
        new OpenAIChatRequestBuilder()
            .addSystemMessage(preservedNoteContent)
            .responseJsonSchema(tool);

    List<ChatCompletionMessageParam> messages = builder.buildMessages();

    // Add the expected response as an assistant message
    ChatCompletionMessageParam assistantMessage =
        ChatCompletionMessageParam.ofAssistant(
            ChatCompletionAssistantMessageParam.builder()
                .content(new ObjectMapperConfig().objectMapper().valueToTree(argument).toString())
                .build());
    messages.add(assistantMessage);

    return OpenAIChatGPTFineTuningExample.from(messages);
  }

  @JsonIgnore
  private QuestionEvaluation getQuestionEvaluation() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.improvementAdvices = comment;
    questionEvaluation.feasibleQuestion = isPositiveFeedback;
    questionEvaluation.correctChoices = getRealCorrectAnswerIndice();
    return questionEvaluation;
  }

  @JsonIgnore
  private int[] getRealCorrectAnswerIndice() {
    if (isPositiveFeedback) {
      return new int[] {getPreservedQuestion().getF1__correctChoiceIndex()};
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
