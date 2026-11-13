package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class NoteExportControllerPropertyHistoryTests extends ControllerTestBase {
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
  void filtersBeforeDistinctNewestTenUsingExactKeyAndValueIncludingUnansweredAndInactive()
      throws Exception {
    MemoryTracker run =
        makeMe
            .aMemoryTrackerFor(note)
            .propertyKey("example of")
            .propertyValue("[[run]]")
            .removedFromTracking()
            .please();
    Timestamp time = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
    for (int i = 0; i < 12; i++) {
      Mcq mcq = makeMe.anMcq().stem("matching-" + i).createdAt(time).please();
      makeMe.aRecallPrompt().forMemoryTracker(run).withMcq(mcq).please();
      makeMe.aRecallPrompt().forMemoryTracker(run).withMcq(mcq).please();
    }
    question("example of", "[[walk]]", "newer-walk");
    question("Example of", "[[run]]", "different-key-case", makeMe.aUser().please());
    question("example of", "[[Run]]", "different-value-case", makeMe.aUser().please());
    String markdown = markdown(request("example of").param("propertyValue", "[[run]]"));
    List<String> stems =
        markdown.lines().filter(line -> line.matches("\\d+\\. matching-.*")).toList();
    assertThat(
        stems,
        contains(
            "1. matching-11",
            "2. matching-10",
            "3. matching-9",
            "4. matching-8",
            "5. matching-7",
            "6. matching-6",
            "7. matching-5",
            "8. matching-4",
            "9. matching-3",
            "10. matching-2"));
    assertThat(
        markdown,
        not(
            anyOf(
                containsString("newer-walk"),
                containsString("different-key-case"),
                containsString("different-value-case"))));
  }

  @Test
  void creationTimePrecedesIdForMatchingHistory() throws Exception {
    MemoryTracker run =
        makeMe.aMemoryTrackerFor(note).propertyKey("example of").propertyValue("[[run]]").please();
    Mcq newer =
        makeMe
            .anMcq()
            .stem("newer-first")
            .createdAt(Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")))
            .please();
    Mcq older =
        makeMe
            .anMcq()
            .stem("older-second")
            .createdAt(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")))
            .please();
    makeMe.aRecallPrompt().forMemoryTracker(run).withMcq(newer).please();
    makeMe.aRecallPrompt().forMemoryTracker(run).withMcq(older).please();
    assertThat(
        markdown(request("example of").param("propertyValue", "[[run]]")),
        stringContainsInOrder("1. newer-first", "2. older-second"));
  }

  private Mcq question(String key, String value, String stem) {
    return question(key, value, stem, currentUser.getUser());
  }

  private Mcq question(String key, String value, String stem, User learner) {
    Mcq mcq = makeMe.anMcq().stem(stem).please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(note).by(learner).propertyKey(key).propertyValue(value).please();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(mcq).please();
    return mcq;
  }

  private MockHttpServletRequestBuilder request(String key) {
    return get("/api/notes/{note}/ai-context-markdown", note.getId())
        .param("tokenLimit", "5000")
        .param("includeRecentQuestions", "true")
        .param("propertyKey", key);
  }

  private String markdown(MockHttpServletRequestBuilder request) throws Exception {
    String response =
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new ObjectMapper().readTree(response).get("markdown").asText();
  }
}
