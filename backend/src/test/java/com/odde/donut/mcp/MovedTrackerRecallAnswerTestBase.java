package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.Note;
import com.odde.donut.services.NoteReferenceService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

abstract class MovedTrackerRecallAnswerTestBase extends ListRecallAnswersMcpEndpointTestBase {
  @Autowired NoteReferenceService noteReferenceService;
  @Autowired EntityManager entityManager;

  Asked answerAndExclude(Note input, String propertyKey) throws Exception {
    currentUser.setUser(personalUser);
    int originalId = input.getId();
    var tracker =
        makeMe
            .aMemoryTrackerFor(input)
            .by(personalUser)
            .propertyKey(propertyKey)
            .assimilatedAt(hoursAgo(48))
            .stabilityAndNextRecallAt(200f)
            .difficulty(6f)
            .please();
    var question =
        makeMe
            .anMcq()
            .stem("Which approach?")
            .choices("key", "second", "selected-third")
            .correctAnswerIndex(0)
            .validationRationale("The key fits.")
            .inMemoryPlease();
    var prompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(tracker)
            .withMcq(question)
            .questionGeneratedTime(hoursAgo(25))
            .please();
    int trackerId = tracker.getId();
    int promptId = prompt.getId();
    int questionId = prompt.getMcq().getId();
    testabilitySettings.timeTravelTo(hoursAgo(24));
    json(
        post("/api/recall-prompts/{id}/answer", promptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"choiceIndex\":2,\"thinkingTimeMs\":1200,\"awayMs\":200,\"awayCount\":1,\"detourMs\":300,\"detourCount\":2,\"idleMs\":400}"));
    testabilitySettings.timeTravelTo(hoursAgo(12));
    json(
        post("/api/recall-prompts/{id}/exclude-as-ambiguous", promptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"two interpretations\"}"));
    testabilitySettings.timeTravelTo(now);
    JsonNode before = history(trackerId);
    assertThat(before.size(), equalTo(1));
    JsonNode schedule = json(get("/api/memory-trackers/{id}", trackerId));
    assertThat(schedule.at("/note/id").asInt(), equalTo(originalId));
    assertThat(schedule.path("propertyKey").asText(), equalTo(propertyKey));
    legacyAskedInput(questionId, originalId);
    return new Asked(originalId, trackerId, promptId, questionId, before, schedule);
  }

  void legacyAskedInput(int questionId, int originalId) {
    makeMe.entityPersister.flushAndClear();
    entityManager
        .createNativeQuery("UPDATE mcq SET note_id = :inputId WHERE id = :questionId")
        .setParameter("inputId", originalId)
        .setParameter("questionId", questionId)
        .executeUpdate();
    Number stored =
        (Number)
            entityManager
                .createNativeQuery("SELECT note_id FROM mcq WHERE id = :questionId")
                .setParameter("questionId", questionId)
                .getSingleResult();
    assertThat(stored.intValue(), equalTo(originalId));
    entityManager.clear();
  }

  void retained(Asked asked, int noteId, String propertyKey) throws Exception {
    assertThat(history(asked.trackerId()), equalTo(asked.history()));
    JsonNode tracker = json(get("/api/memory-trackers/{id}", asked.trackerId()));
    assertThat(tracker.path("id").asInt(), equalTo(asked.trackerId()));
    assertThat(tracker.at("/note/id").asInt(), equalTo(noteId));
    assertThat(tracker.path("propertyKey").asText(), equalTo(propertyKey));
    assertThat(tracker.path("propertyValue").asText(), equalTo(""));
    for (String field :
        List.of(
            "stability",
            "difficulty",
            "lastRecalledAt",
            "nextRecallAt",
            "assimilatedAt",
            "removedFromTracking",
            "type")) {
      assertThat(field, tracker.get(field), equalTo(asked.tracker().get(field)));
    }
  }

  void currentEvidence(JsonNode attempt, String title) {
    assertThat(attempt.at("/note/title").asText(), equalTo(title));
    assertThat(attempt.at("/note/portablePath").asText(), equalTo("Engineering:" + title));
    assertThat(
        attempt.at("/note/graph/focusNote/portablePath").asText(), equalTo("Engineering:" + title));
  }

  JsonNode history(int trackerId) throws Exception {
    return json(get("/api/memory-trackers/{id}/recall-history", trackerId));
  }

  JsonNode answers() throws Exception {
    makeMe.entityPersister.flushAndClear();
    return listRecallAnswers();
  }

  String export(int noteId, String propertyKey) throws Exception {
    var request =
        get("/api/notes/{id}/ai-context-markdown", noteId)
            .param("tokenLimit", "5000")
            .param("includeRecentQuestions", "true");
    if (!propertyKey.isEmpty()) {
      request.param("propertyKey", propertyKey);
    }
    return json(request).path("markdown").asText();
  }

  JsonNode json(MockHttpServletRequestBuilder request) throws Exception {
    makeMe.entityPersister.flushAndClear();
    String body =
        mockMvc
            .perform(authorized(request))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return body.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(body);
  }

  record Asked(
      int originalId,
      int trackerId,
      int promptId,
      int questionId,
      JsonNode history,
      JsonNode tracker) {}
}
