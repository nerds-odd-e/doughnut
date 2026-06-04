package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationRequestBuilderTests {

  @Autowired MakeMe makeMe;
  @Autowired NoteQuestionGenerationService noteQuestionGenerationService;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @TestBean CurrentUser currentUser;

  private User user;

  static CurrentUser currentUser() {
    return new CurrentUser();
  }

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentUser.setUser(user);
  }

  private String instructionText(StructuredResponseCreateParams<MCQWithAnswer> request) {
    return request.rawParams().instructions().orElse("");
  }

  private String inputText(StructuredResponseCreateParams<MCQWithAnswer> request) {
    return request.rawParams().input().flatMap(input -> input.text()).orElse("");
  }

  @Test
  void shouldIncludePropertyFocusInstructionKeyValueAndLinkTargets() {
    Note target = makeMe.aNote().title("Heart").notebookOwnedBy(user).please();
    String markdown =
        "---\n"
            + "a part of: Circulatory system includes [[Heart]]\n"
            + "---\n"
            + "The human body overview.\n";
    Note focus = makeMe.aNote().notebook(target.getNotebook()).content(markdown).please();
    wikiTitleCacheService.refreshForNote(focus, user);

    StructuredResponseCreateParams<MCQWithAnswer> request =
        noteQuestionGenerationService.buildQuestionGenerationRequest(focus, null, "a part of");

    String instructions = instructionText(request);
    assertThat(
        instructions,
        containsString(QuestionGenerationRequestBuilder.PROPERTY_FOCUS_INSTRUCTION_HEADER));
    assertThat(instructions, containsString("Focus on property \"a part of\""));
    assertThat(instructions, containsString("Property key: a part of"));
    assertThat(
        instructions, containsString("Property value: Circulatory system includes [[Heart]]"));

    String focusContext = inputText(request);
    assertThat(focusContext, containsString("# Focus Context"));
    assertThat(focusContext, containsString("Heart"));
  }
}
