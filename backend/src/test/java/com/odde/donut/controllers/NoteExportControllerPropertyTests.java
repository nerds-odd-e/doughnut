package com.odde.donut.controllers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class NoteExportControllerPropertyTests extends ControllerTestBase {
  @Autowired MockMvc mockMvc;
  Note note;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note =
        makeMe
            .aNote("Movement")
            .notebookOwnedBy(currentUser.getUser())
            .content(
                "---\nkind: Motion\nexample of: [\"[[walk]]\", \"[[run]]\", \"[[run]]\"]\n---\nSurrounding explanation")
            .please();
  }

  @Test
  void scalarFocusKeepsContextAndOnlyItsAttributedQuestions() throws Exception {
    question("kind", "", "scalar-question");
    question("example of", "[[run]]", "run-question");
    mockMvc
        .perform(request("kind"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Property value: Motion")))
        .andExpect(jsonPath("$.markdown", containsString("Surrounding explanation")))
        .andExpect(jsonPath("$.markdown", containsString("scalar-question")))
        .andExpect(jsonPath("$.markdown", not(containsString("run-question"))));
  }

  @Test
  void secondDuplicateDescribesOccurrenceAndSharedHistoryWithWikiTargets() throws Exception {
    question("example of", "[[run]]", "run-question");
    mockMvc
        .perform(
            request("example of")
                .param("propertyValue", "[[run]]")
                .param("propertyOccurrence", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Property value: [[run]]")))
        .andExpect(jsonPath("$.markdown", containsString("Highlighted item occurrence: 2")))
        .andExpect(jsonPath("$.markdown", containsString("history is shared")))
        .andExpect(jsonPath("$.markdown", containsString("Surrounding explanation")))
        .andExpect(jsonPath("$.markdown", containsString("[[walk]]")))
        .andExpect(jsonPath("$.markdown", containsString("run-question")));
  }

  @Test
  void wholeListContainsQuestionsAcrossRawValues() throws Exception {
    question("example of", "[[walk]]", "walk-question");
    question("example of", "[[run]]", "run-question");
    mockMvc
        .perform(request("example of"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("the whole list property")))
        .andExpect(jsonPath("$.markdown", containsString("walk-question")))
        .andExpect(jsonPath("$.markdown", containsString("run-question")));
  }

  @Test
  void wikiTargetsSurviveAZeroBodyBudget() throws Exception {
    Note walk = makeMe.aNote("walk").notebook(note.getNotebook()).please();
    mockMvc
        .perform(request("example of", 0, true).param("propertyValue", "[[run]]"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.markdown", containsString("walk (resolved note id: " + walk.getId() + ")")));
  }

  @Test
  void noHistoryStillReturnsFocusedContext() throws Exception {
    question("example of", "[[walk]]", "walk-question");
    mockMvc
        .perform(request("example of").param("propertyValue", "[[run]]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Property value: [[run]]")))
        .andExpect(jsonPath("$.markdown", not(containsString("Recent multiple-choice questions"))));
  }

  @Test
  void uncheckedQuestionOptionKeepsFocus() throws Exception {
    question("kind", "", "scalar-question");
    mockMvc
        .perform(request("kind", 5000, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Property value: Motion")))
        .andExpect(jsonPath("$.markdown", not(containsString("scalar-question"))));
  }

  @ParameterizedTest
  @CsvSource({
    "missing,,",
    "example of,[[gone]],1",
    "example of,[[run]],3",
    "example of,[[run]],0",
    "kind,Motion,1",
    "kind,,2"
  })
  void unresolvedSelectorsReturnActionableFeedback(String key, String value, String occurrence)
      throws Exception {
    var request = request(key);
    if (value != null) request.param("propertyValue", value);
    if (occurrence != null) request.param("propertyOccurrence", occurrence);
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("cannot be resolved")))
        .andExpect(jsonPath("$.message", containsString("Close export")));
  }

  private Mcq question(String key, String value, String stem) {
    Mcq mcq = makeMe.anMcq().forNote(note).stem(stem).please();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(currentUser.getUser())
            .propertyKey(key)
            .propertyValue(value)
            .please();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(mcq).please();
    return mcq;
  }

  private MockHttpServletRequestBuilder request(String key) {
    return request(key, 5000, true);
  }

  private MockHttpServletRequestBuilder request(
      String key, int tokenLimit, boolean includeQuestions) {
    return get("/api/notes/{note}/ai-context-markdown", note.getId())
        .param("tokenLimit", Integer.toString(tokenLimit))
        .param("includeRecentQuestions", Boolean.toString(includeQuestions))
        .param("propertyKey", key);
  }
}
