package com.odde.doughnut.services.ai.tools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import org.junit.jupiter.api.Test;

class AiToolFactoryTest {

  @Test
  void questionAiToolContainsMergedQuestionDesignerInstruction() {
    InstructionAndSchema result = AiToolFactory.questionAiTool();

    assertThat(result.getMessageBody(), containsString("Question Designer"));
    assertThat(result.getMessageBody(), containsString("focus note"));
    assertThat(result.getMessageBody(), containsString("**MCQ format**"));
    assertThat(result.getMessageBody(), containsString("3 choices"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void shouldMaintainBackwardCompatibilityWithMcqMethod() {
    InstructionAndSchema result = AiToolFactory.mcqWithAnswerAiTool();

    assertThat(result.getMessageBody(), containsString("3 choices"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void mcqWithAnswerAiToolShouldContainBaseInstruction() {
    InstructionAndSchema result = AiToolFactory.mcqWithAnswerAiTool();

    assertThat(result.getMessageBody(), containsString("Question Designer"));
    assertThat(result.getMessageBody(), containsString("focus note"));
    assertThat(result.getMessageBody(), containsString("hidden context"));
  }
}
