package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class ListNoteCandidatesMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  @Test
  void returnsAliasMatchAsCandidate() throws Exception {
    Note aliasMatch =
        makeMe.aNote("Colour Theory").notebookOwnedBy(personalUser).aliases("color").please();

    JsonNode candidates = callListNoteCandidates(personalToken, "color");

    assertThat(candidateIds(candidates), hasItem(aliasMatch.getId()));
  }

  @Test
  void includesSubscribedNotebookNotes() throws Exception {
    User publisher = makeMe.aUser().please();
    Note subscribed = makeMe.aNote("viva la subscription").notebookOwnedBy(publisher).please();
    makeMe.aBazaarNotebook(subscribed.getNotebook()).please();
    makeMe.aSubscription().forNotebook(subscribed.getNotebook()).forUser(personalUser).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "viva la");

    assertThat(candidateIds(candidates), hasItem(subscribed.getId()));
  }

  @Test
  void equalTitlesAreDisambiguatedByNotebookAndFolder() throws Exception {
    Notebook cs = makeMe.aNotebook().creatorAndOwner(personalUser).name("CS concepts").please();
    Folder programming = makeMe.aFolder().notebook(cs).name("Programming Concepts").please();
    Note inFolder = makeMe.aNote().title("Functional").folder(programming).please();
    Notebook maths = makeMe.aNotebook().creatorAndOwner(personalUser).name("Maths").please();
    Note atRoot = makeMe.aNote().title("Functional").notebook(maths).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "Functional");

    assertThat(candidates.size(), equalTo(2));
    JsonNode first = findCandidate(candidates, inFolder.getId());
    JsonNode second = findCandidate(candidates, atRoot.getId());
    assertThat(first.get("notebookName").asText(), equalTo("CS concepts"));
    assertThat(first.get("folderName").asText(), equalTo("Programming Concepts"));
    assertThat(second.get("notebookName").asText(), equalTo("Maths"));
    assertThat(second.has("folderName"), equalTo(false));
  }

  @Test
  void emptySearchSignalWhenNothingMatches() throws Exception {
    mockMvc
        .perform(authorized(mcpPost(toolsCallBody("nonexistent-note-xyz"))))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.result.content[0].text", equalTo(ListNoteCandidatesTool.EMPTY_SEARCH_SIGNAL)));
  }

  @Test
  void twoTokensStayIsolatedAcrossInterleavedHttpCalls() throws Exception {
    User userA = personalUser;
    UserToken tokenA = personalToken;
    User userB = makeMe.aUser().please();
    UserToken tokenB = makeMe.aUserToken().forUser(userB).withLabel("mcp-b").please();
    Note noteA = makeMe.aNote("AlphaOnlySecret").notebookOwnedBy(userA).please();
    Note noteB = makeMe.aNote("BetaOnlySecret").notebookOwnedBy(userB).please();
    Note privateForeign =
        makeMe.aNote("AlphaOnlySecret").notebookOwnedBy(makeMe.aUser().please()).please();

    // Poison test CurrentUser so an identity leak would surface as A's notes on B's token.
    currentUser.setUser(userA);

    JsonNode bLookingForA = callListNoteCandidates(tokenB, "AlphaOnlySecret");
    JsonNode aLookingForA = callListNoteCandidates(tokenA, "AlphaOnlySecret");
    JsonNode aLookingForB = callListNoteCandidates(tokenA, "BetaOnlySecret");
    JsonNode bLookingForB = callListNoteCandidates(tokenB, "BetaOnlySecret");

    assertThat(candidateIds(bLookingForA), hasSize(0));
    assertThat(candidateIds(aLookingForA), contains(noteA.getId()));
    assertThat(candidateIds(aLookingForA), not(hasItem(privateForeign.getId())));
    assertThat(candidateIds(aLookingForB), hasSize(0));
    assertThat(candidateIds(bLookingForB), contains(noteB.getId()));
  }

  private JsonNode callListNoteCandidates(UserToken token, String query) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                mcpPost(toolsCallBody(query))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken()))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    String text = root.at("/result/content/0/text").asText();
    if (ListNoteCandidatesTool.EMPTY_SEARCH_SIGNAL.equals(text)) {
      return objectMapper.createArrayNode();
    }
    return objectMapper.readTree(text);
  }

  private static String toolsCallBody(String query) {
    return """
        {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{\
        "name":"%s","arguments":{"query":%s}}}
        """
        .formatted(ListNoteCandidatesTool.NAME, quoteJson(query));
  }

  private static String quoteJson(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  private static List<Integer> candidateIds(JsonNode candidates) {
    List<Integer> ids = new ArrayList<>();
    for (JsonNode candidate : candidates) {
      ids.add(candidate.get("noteId").asInt());
    }
    return ids;
  }

  private static JsonNode findCandidate(JsonNode candidates, Integer noteId) {
    for (JsonNode candidate : candidates) {
      if (candidate.get("noteId").asInt() == noteId) {
        return candidate;
      }
    }
    throw new AssertionError("candidate not found: " + noteId);
  }
}
