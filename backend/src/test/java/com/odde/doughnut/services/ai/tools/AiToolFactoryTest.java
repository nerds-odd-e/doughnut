package com.odde.doughnut.services.ai.tools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.services.ai.GeneratedMcq;
import org.junit.jupiter.api.Test;

class AiToolFactoryTest {

  @Test
  void questionAiToolContainsMergedQuestionDesignerInstruction() {
    InstructionAndSchema result = AiToolFactory.mcqAiTool(false);

    assertThat(result.getMessageBody(), containsString("Question Designer"));
    assertThat(result.getMessageBody(), containsString("focus note"));
    assertThat(result.getMessageBody(), containsString("memory-stimulating, single-answer MCQ"));
    assertThat(result.getMessageBody(), containsString("exactly three distractors"));
    assertThat(result.getMessageBody(), containsString("The learner cannot see this context"));
    assertThat(result.getParameterClass(), equalTo(GeneratedMcq.class));
  }

  @Test
  void questionAiToolWhenFocusNoteContentEmptyGroundsAnswerInTitleOrDirectReferences() {
    InstructionAndSchema result = AiToolFactory.mcqAiTool(true);

    assertThat(result.getMessageBody(), containsString("its title or direct references"));
    assertThat(result.getMessageBody(), not(containsString("Focus Note title or content")));
  }
}
