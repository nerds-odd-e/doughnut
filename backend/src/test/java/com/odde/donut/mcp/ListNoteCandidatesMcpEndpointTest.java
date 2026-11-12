package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    Folder design = makeMe.aFolder().parentFolder(practices).name("Design").please();
    makeMe.aNote("Colour Theory").folder(design).aliases("color").please();
    makeMe.aNote("Colour Theory").notebook(notebook).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "color");

    JsonNode candidate = findCandidate(candidates, "Engineering:Practices/Design/Colour Theory");
    assertThat(
        candidate.get("portablePath").asText(),
        equalTo("Engineering:Practices/Design/Colour Theory"));
    assertThat(candidate.get("folderPath").asText(), equalTo("Practices/Design"));
  }

  @Test
  void includesSubscribedNotebookNotes() throws Exception {
    User publisher = makeMe.aUser().please();
    Note subscribed = makeMe.aNote("viva la subscription").notebookOwnedBy(publisher).please();
    makeMe.aBazaarNotebook(subscribed.getNotebook()).please();
    makeMe.aSubscription().forNotebook(subscribed.getNotebook()).forUser(personalUser).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "viva la");

    assertThat(
        candidatePaths(candidates),
        hasItem(subscribed.getNotebook().getName() + ":viva la subscription"));
  }

  @Test
  void equalTitlesAreDisambiguatedByNotebookAndFolder() throws Exception {
    Notebook cs = makeMe.aNotebook().creatorAndOwner(personalUser).name("CS concepts").please();
    Folder programming = makeMe.aFolder().notebook(cs).name("Programming Concepts").please();
    makeMe.aNote().title("Functional").folder(programming).please();
    Notebook maths = makeMe.aNotebook().creatorAndOwner(personalUser).name("Maths").please();
    makeMe.aNote().title("Functional").notebook(maths).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "Functional");

    assertThat(candidates.size(), equalTo(2));
    JsonNode first = findCandidate(candidates, "CS concepts:Functional");
    JsonNode second = findCandidate(candidates, "Maths:Functional");
    assertThat(first.get("notebookName").asText(), equalTo("CS concepts"));
    assertThat(first.get("folderName").asText(), equalTo("Programming Concepts"));
    assertThat(second.get("notebookName").asText(), equalTo("Maths"));
    assertThat(second.get("portablePath").asText(), equalTo("Maths:Functional"));
    assertThat(second.get("folderPath").asText(), equalTo(""));
    assertThat(second.has("folderName"), equalTo(false));
  }

  @Test
  void rootCandidateUsesExactRootPathWhenTitleIsAmbiguous() throws Exception {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    makeMe.aNote("Code Review").notebook(notebook).please();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote("Code Review").folder(practices).please();

    JsonNode candidates = callListNoteCandidates(personalToken, "Code Review");

    assertThat(
        findCandidate(candidates, "Engineering:/Code Review").get("portablePath").asText(),
        equalTo("Engineering:/Code Review"));
  }

  @Test
  void candidatePathEscapesWikiDestinationCharacters() throws Exception {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    makeMe.aNote("A|B\\C").notebook(notebook).aliases("escaped destination").please();

    JsonNode candidates = callListNoteCandidates(personalToken, "escaped destination");

    assertThat(candidates.get(0).get("portablePath").asText(), equalTo("Engineering:A\\|B\\\\C"));
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
    makeMe.aNote("AlphaOnlySecret").notebookOwnedBy(makeMe.aUser().please()).please();

    // Poison test CurrentUser so an identity leak would surface as A's notes on B's token.
    currentUser.setUser(userA);

    JsonNode bLookingForA = callListNoteCandidates(tokenB, "AlphaOnlySecret");
    JsonNode aLookingForA = callListNoteCandidates(tokenA, "AlphaOnlySecret");
    JsonNode aLookingForB = callListNoteCandidates(tokenA, "BetaOnlySecret");
    JsonNode bLookingForB = callListNoteCandidates(tokenB, "BetaOnlySecret");

    assertThat(candidatePaths(bLookingForA), hasSize(0));
    assertThat(
        candidatePaths(aLookingForA), contains(noteA.getNotebook().getName() + ":AlphaOnlySecret"));
    assertThat(candidatePaths(aLookingForB), hasSize(0));
    assertThat(
        candidatePaths(bLookingForB), contains(noteB.getNotebook().getName() + ":BetaOnlySecret"));
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

  private static List<String> candidatePaths(JsonNode candidates) {
    List<String> paths = new ArrayList<>();
    for (JsonNode candidate : candidates) {
      paths.add(candidate.get("portablePath").asText());
    }
    return paths;
  }

  private static JsonNode findCandidate(JsonNode candidates, String portablePath) {
    for (JsonNode candidate : candidates) {
      if (candidate.get("portablePath").asText().equals(portablePath)) {
        return candidate;
      }
    }
    throw new AssertionError("candidate not found: " + portablePath);
  }
}
