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
    assertThat(result.getMessageBody(), containsString("memory-stimulating, single-answer MCQ"));
    assertThat(result.getMessageBody(), containsString("exactly 4 choices"));
    assertThat(result.getParameterClass(), equalTo(MCQWithAnswer.class));
  }

  @Test
  void mcqWithAnswerAiToolShouldContainBaseInstruction() {
    InstructionAndSchema result = AiToolFactory.mcqWithAnswerAiTool();

    assertThat(result.getMessageBody(), containsString("Question Designer"));
    assertThat(result.getMessageBody(), containsString("focus note"));
    assertThat(result.getMessageBody(), containsString("The learner cannot see this context"));
  }

  @Test
  void questionAiToolWhenFocusNoteEmptyGroundsAnswerInTitleOrDirectReferences() {
    InstructionAndSchema result = AiToolFactory.questionAiTool(true);

    assertThat(result.getMessageBody(), containsString("its title or direct references"));
    assertThat(result.getMessageBody(), not(containsString("Focus Note title or content")));
  }
}
