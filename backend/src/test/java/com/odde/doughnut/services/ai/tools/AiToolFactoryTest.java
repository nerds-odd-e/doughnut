package com.odde.doughnut.services.ai.tools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import org.junit.jupiter.api.Test;

class AiToolFactoryTest {

  @Test
  void shouldIncludeCustomPromptInInstruction() {
    String customPrompt = "Generate a True/False question with choices ['True', 'False']";
    InstructionAndSchema result = AiToolFactory.questionAiTool(customPrompt, null, null);

    assertThat(result.getMessageBody(), containsString(customPrompt));
    assertThat(result.getMessageBody(), containsString("Please act as a Question Designer"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void shouldHandleNullCustomPrompt() {
    InstructionAndSchema result = AiToolFactory.questionAiTool(null, null, null);

    assertThat(result.getMessageBody(), containsString("Please act as a Question Designer"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void shouldHandleBlankCustomPrompt() {
    InstructionAndSchema result = AiToolFactory.questionAiTool("   ", null, null);

    assertThat(result.getMessageBody(), containsString("Please act as a Question Designer"));
    assertThat(result.getMessageBody(), not(containsString("   \n")));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void shouldMaintainBackwardCompatibilityWithMcqMethod() {
    InstructionAndSchema result = AiToolFactory.mcqWithAnswerAiTool();

    assertThat(result.getMessageBody(), containsString("Multiple-Choice Question"));
    assertThat(result.getMessageBody(), containsString("2 to 3 options"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void mcqWithAnswerAiToolShouldContainBaseInstruction() {
    InstructionAndSchema result = AiToolFactory.mcqWithAnswerAiTool();

    assertThat(result.getMessageBody(), containsString("Please act as a Question Designer"));
    assertThat(result.getMessageBody(), containsString("Focus on the Focus Note"));
    assertThat(result.getMessageBody(), containsString("Leverage the Extended Graph"));
    assertThat(result.getMessageBody(), containsString("Ensure Question Self-Sufficiency"));
  }
}
