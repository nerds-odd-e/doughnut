package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import tools.jackson.databind.JsonNode;

class AssistantMcpScopedAccessTest extends AssistantMcpTestSupport {
  @Test
  void currentGrantReadsAsTheConsentingUserAcrossInterleavedRequests() throws Exception {
    User first = makeMe.aUser("another_old_learner").please();
    var firstNote = makeMe.aNote("First secret").notebookOwnedBy(first).please();
    var second =
        makeMe
            .aNote("Second secret")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    String secondToken = issueAccessToken();
    session = signInAs("another_old_learner");
    String firstToken = issueAccessToken();

    assertThat(read(firstToken, firstNote).get("title").asString(), is("First secret"));
    assertThat(read(secondToken, second).get("title").asString(), is("Second secret"));
    call(
            firstToken,
            "read_note",
            Map.of("portablePath", second.getNotebook().getName() + ":" + second.getTitle()))
        .andExpect(jsonPath("$.result.isError").value(true));
    assertThat(read(firstToken, firstNote).get("title").asString(), is("First secret"));
  }

  @Test
  void reportCapableAssistantCanReadWithItsConsentedScopes() throws Exception {
    var note =
        makeMe
            .aNote("Session instructions")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    var reply =
        exchange(
                approvedCode("notes:read learning_session:record"),
                Map.of(),
                "test-assistant-secret")
            .andExpect(status().isOk())
            .andReturn();
    var credentials = objectMapper.readTree(reply.getResponse().getContentAsString());
    String token = credentials.get("access_token").asString();
    assertThat(
        authorizations
            .findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
            .getAccessToken()
            .getToken()
            .getScopes(),
        is(Set.of("notes:read", "learning_session:record")));
    assertThat(read(token, note).get("title").asString(), is("Session instructions"));
  }

  private JsonNode read(String token, Note note) throws Exception {
    var response =
        call(
                token,
                "read_note",
                Map.of("portablePath", note.getNotebook().getName() + ":" + note.getTitle()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(
        objectMapper
            .readTree(response.getContentAsString())
            .at("/result/content/0/text")
            .asString());
  }
}
