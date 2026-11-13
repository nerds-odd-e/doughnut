package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

class ListRecallAnswersMovedTrackerMcpEndpointTest extends MovedTrackerRecallAnswerTestBase {
  @Test
  void keepsTheAnsweredQuestionWhenItsRelationshipBecomesASourceProperty() throws Exception {
    int sourceId = note.getId();
    Note target = makeMe.aNote("Target").underSameNotebookAs(note).please();
    Note relationship =
        makeMe
            .aNote("Relationship")
            .underSameNotebookAs(note)
            .asRelationship("related", note, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relationship);
    Asked asked = answerAndExclude(relationship, "");
    JsonNode prompt = asked.history().at("/0/recallPrompt");
    JsonNode answer = prompt.path("answer");
    JsonNode log = asked.history().at("/0/recallLog");
    assertThat(prompt.path("id").asInt(), equalTo(asked.promptId()));
    assertThat(prompt.at("/mcq/id").asInt(), equalTo(asked.questionId()));
    assertThat(prompt.at("/mcq/questionStem").asText(), equalTo("Which approach?"));
    assertThat(
        texts(prompt.at("/mcq/responseChoices")), contains("key", "second", "selected-third"));
    assertThat(prompt.at("/mcq/correctAnswerIndex").asInt(), equalTo(0));
    assertThat(answer.path("id").asInt(), greaterThan(0));
    assertThat(answer.path("choiceIndex").asInt(), equalTo(2));
    assertThat(answer.path("correct").asBoolean(), equalTo(false));
    assertThat(answer.path("outcome").isNull(), equalTo(true));
    assertThat(answer.path("exclusionReason").asText(), equalTo("two interpretations"));
    assertThat(
        Instant.parse(answer.path("excludedAt").asText()), equalTo(hoursAgo(12).toInstant()));
    assertThat(
        Instant.parse(prompt.path("answerTime").asText()), equalTo(hoursAgo(24).toInstant()));
    assertThat(
        Instant.parse(prompt.path("questionGeneratedTime").asText()),
        equalTo(hoursAgo(25).toInstant()));
    assertThat(answer.path("thinkingTimeMs").asInt(), equalTo(1200));
    assertThat(answer.path("awayMs").asInt(), equalTo(200));
    assertThat(answer.path("awayCount").asInt(), equalTo(1));
    assertThat(answer.path("detourMs").asInt(), equalTo(300));
    assertThat(answer.path("detourCount").asInt(), equalTo(2));
    assertThat(answer.path("idleMs").asInt(), equalTo(400));
    assertThat(log.path("id").asInt(), greaterThan(0));
    assertThat(log.path("answerId"), equalTo(answer.path("id")));
    assertThat(log.path("memoryTrackerId").asInt(), equalTo(asked.trackerId()));
    assertThat(log.path("productOutcome").asText(), equalTo("AGAIN"));
    assertThat(log.path("recordedAt"), equalTo(prompt.path("answerTime")));
    assertThat(log.path("elapsedHours").asInt(), equalTo(24));
    assertThat(log.path("stabilityBefore").asDouble(), equalTo(200d));
    assertThat(log.path("difficultyBefore").asDouble(), equalTo(6d));
    assertThat(log.path("retrievability").asDouble(), greaterThan(0d));

    json(post("/api/relations/{id}/reduce-to-source-property", asked.originalId()));

    retained(asked, sourceId, "related");
    JsonNode attempt = answers().at("/attempts/0");
    currentEvidence(attempt, "Code Review");
    assertThat(attempt.path("correct").asBoolean(), equalTo(false));
    assertThat(attempt.path("answeredAt").asText(), equalTo(hoursAgo(24).toInstant().toString()));
    assertThat(
        attempt.at("/multipleChoice/validationRationale").asText(), equalTo("The key fits."));
    assertThat(attempt.at("/multipleChoice/stem").asText(), equalTo("Which approach?"));
    assertThat(
        texts(attempt.at("/multipleChoice/choices")), contains("key", "second", "selected-third"));
    assertThat(attempt.at("/multipleChoice/selectedChoice").asText(), equalTo("selected-third"));
    assertThat(attempt.at("/multipleChoice/expectedAnswer").asText(), equalTo("key"));
    assertThat(attempt.at("/excludedAsAmbiguous/reason").asText(), equalTo("two interpretations"));
    String markdown = export(sourceId, "related");
    assertThat(markdown, containsString("1. Which approach?\n"));
    for (String choice : List.of("key", "second", "selected-third")) {
      assertThat(markdown, containsString("- " + choice));
    }
    assertThat(markdown, containsString("Donut answer key: key"));
    assertThat(markdown, containsString("Learner's selected answer: selected-third"));

    var ai = new OpenAiStructuredResponseMock(officialClient);
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.correctChoices = new int[] {0};
    evaluation.feasibleQuestion = true;
    evaluation.improvementAdvices = "";
    ai.stubStructuredResponse(evaluation);
    assertThat(
        json(post("/api/recall-prompts/{id}/contest", asked.promptId()))
            .path("rejected")
            .asBoolean(),
        equalTo(true));
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<QuestionEvaluation>> capture =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(ai.responseService()).create(capture.capture());
    String focus = "<focus_note>\nTitle: Code Review\n";
    assertThat(
        capture.getValue().rawParams().input().flatMap(input -> input.text()).orElse(""),
        containsString(focus));
    assertThat(
        history(asked.trackerId()).at("/0/recallPrompt/mcq/id").asInt(),
        equalTo(asked.questionId()));
    JsonNode conversation = json(post("/api/conversation/recall-prompt/{id}", asked.promptId()));
    assertThat(conversation.at("/subject/recallPrompt/id").asInt(), equalTo(asked.promptId()));
    assertThat(
        conversation.at("/subject/recallPrompt/memoryTrackerId").asInt(),
        equalTo(asked.trackerId()));
    assertThat(
        conversation.at("/subject/recallPrompt/recalledNote/noteTopology/id").asInt(),
        equalTo(sourceId));
    assertThat(
        conversation.at("/subject/recallPrompt/recalledNote/propertyKey").asText(),
        equalTo("related"));
    assertThat(
        json(get("/api/conversation/{id}/export", conversation.path("id").asInt()))
            .at("/input/0/content")
            .asText(),
        containsString(focus));
  }

  @Test
  void keepsTheAnsweredPropertyQuestionOnTheReifiedRelationshipAfterDeletingItsSource()
      throws Exception {
    makeMe.theNote(note).content("---\nrelated: \"[[Target]]\"\n---\nbody").please();
    makeMe.aNote("Target").underSameNotebookAs(note).please();
    noteReferenceService.refreshDerivedIndexesForNote(note);
    Asked asked = answerAndExclude(note, "related");
    assertThat(
        asked.history().at("/0/recallPrompt/mcq/questionStem").asText(),
        equalTo("Which approach?"));
    assertThat(asked.history().at("/0/recallPrompt/mcq/id").asInt(), equalTo(asked.questionId()));
    int destinationId =
        json(post("/api/notes/{id}/reify-property", asked.originalId())
                .param("propertyKey", "related"))
            .at("/note/id")
            .asInt();
    assertThat(destinationId, greaterThan(0));
    json(
        post("/api/notes/{id}/trash", asked.originalId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"referenceHandling\":\"LEAVE_DEAD_LINKS\"}"));
    json(post("/api/notes/{id}/permanently-delete", asked.originalId()));

    retained(asked, destinationId, "");
    JsonNode attempt = answers().at("/attempts/0");
    currentEvidence(attempt, "Code Review related Target");
    assertThat(attempt.at("/multipleChoice/stem").asText(), equalTo("Which approach?"));
    assertThat(export(destinationId, ""), containsString("Which approach?"));
  }
}
