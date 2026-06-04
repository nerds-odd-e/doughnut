package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredefinedQuestionServiceRegenerationTest {
  @Mock EntityPersister entityPersister;
  @Mock AiQuestionGenerator aiQuestionGenerator;
  @Mock Note note;

  MCQWithAnswer originalMcq;
  MCQWithAnswer regeneratedMcq;
  QuestionEvaluation contestResult;
  PredefinedQuestionService service;

  @BeforeEach
  void setup() {
    MakeMeWithoutDB makeMe = new MakeMeWithoutDB();
    originalMcq = makeMe.aMCQWithAnswer().please();
    regeneratedMcq = makeMe.aMCQWithAnswer().please();
    contestResult = new QuestionEvaluation();
    when(entityPersister.save(any(PredefinedQuestion.class))).thenAnswer(i -> i.getArgument(0));
    when(entityPersister.merge(any(PredefinedQuestion.class))).thenAnswer(i -> i.getArgument(0));
    service = new PredefinedQuestionService(entityPersister, aiQuestionGenerator, 1);
  }

  @Test
  void savesOriginalAsContestedThenRegeneratedQuestion() {
    when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any(), any()))
        .thenReturn(originalMcq);
    contestResult.feasibleQuestion = true;
    when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
    when(aiQuestionGenerator.regenerateQuestion(any(), any(), any(), any(), any()))
        .thenReturn(regeneratedMcq);

    ArgumentCaptor<PredefinedQuestion> questionCaptor =
        ArgumentCaptor.forClass(PredefinedQuestion.class);

    PredefinedQuestion result = service.generateAFeasibleQuestion(note);

    verify(entityPersister, times(2)).save(questionCaptor.capture());
    PredefinedQuestion firstSaved = questionCaptor.getAllValues().get(0);
    assertThat(firstSaved.getMcqWithAnswer().getQuestion(), equalTo(originalMcq.getQuestion()));
    assertThat(firstSaved.isContested(), is(true));

    PredefinedQuestion secondSaved = questionCaptor.getAllValues().get(1);
    assertThat(secondSaved.getMcqWithAnswer().getQuestion(), equalTo(regeneratedMcq.getQuestion()));
    assertThat(secondSaved.isContested(), is(false));
    assertThat(result.getMcqWithAnswer().getQuestion(), equalTo(regeneratedMcq.getQuestion()));
  }
}
