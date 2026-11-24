package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class GraphNavigationMcpEndpointTest extends McpEndpointTestBase {
  final JsonMapper objectMapper = ObjectMapperConfig.handWrittenJsonMapper();

  private static final String TITLE = "Topic | Tips\\Examples";
  private static final String ESCAPED_TITLE = "Topic \\| Tips\\\\Examples";
  private static final String FOCUS_PATH = "Engineering:/Focus";
  private static final String OUTGOING_PATH = "Engineering:Links/Outgoing/" + ESCAPED_TITLE;
  private static final String INBOUND_PATH = "Engineering:Links/Inbound/" + ESCAPED_TITLE;
  private static final String PEER_PATH = "Engineering:/" + ESCAPED_TITLE;
  private Notebook notebook;
  private String focusContent;

  @BeforeEach
  void graphWithFolderAndRootNamesakes() {
    notebook = makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    Folder links = makeMe.aFolder().notebook(notebook).name("Links").please();
    Folder outgoingFolder = makeMe.aFolder().parentFolder(links).name("Outgoing").please();
    Folder inboundFolder = makeMe.aFolder().parentFolder(links).name("Inbound").please();
    makeMe.aNote(TITLE).folder(outgoingFolder).content("Outgoing target").please();
    makeMe.aNote(TITLE).notebook(notebook).content("Root peer target").please();
    makeMe.aNote("Focus").folder(links).content("Folder focus namesake").please();
    focusContent = "---\nsource: '[Reference](/n1234)'\n---\nSee [[" + OUTGOING_PATH + "]].";
    makeMe.aNote("Focus").notebook(notebook).content(focusContent).please();
    Note inbound = makeMe.aNote(TITLE).folder(inboundFolder).please();
    makeMe.authorReferencingContent(inbound, "Inbound target links to [[" + FOCUS_PATH + "]].");
  }

  @ParameterizedTest
  @CsvSource({
    "outgoingLinks, Outgoing target",
    "inboundReferences, Inbound target links to [[Engineering:/Focus]].",
    "sampleSiblings, Root peer target"
  })
  void navigationReferenceSelectsItsActualTarget(String collection, String content)
      throws Exception {
    JsonNode graph = graph(FOCUS_PATH);
    String expectedPath =
        switch (collection) {
          case "outgoingLinks" -> OUTGOING_PATH;
          case "inboundReferences" -> INBOUND_PATH;
          default -> PEER_PATH;
        };
    String entry = "/focusNote/" + collection + "/0";
    String wiki =
        graph.at(collection.equals("inboundReferences") ? entry + "/link" : entry).asString();

    assertThat(wiki, equalTo("[[" + expectedPath + "]]"));
    assertRoundTrip(wiki, content);
  }

  @Test
  void inboundReferenceCarriesTheReferringNotesPropertyTypesBesideItsLink() throws Exception {
    String sourceContent = "---\nsee also: \"[[Elsewhere]] and [[" + FOCUS_PATH + "]]\"\n---\n";
    Note source = makeMe.aNote("Source").notebook(notebook).please();
    makeMe.authorReferencingContent(source, sourceContent);
    String relationContent =
        "---\ntype: Relationship\nrelation: derives from\nsource: \"[[Source]]\"\ntarget: \"[["
            + FOCUS_PATH
            + "]]\"\n---\n";
    Note relation = makeMe.aNote("Relation").notebook(notebook).please();
    makeMe.authorReferencingContent(relation, relationContent);

    JsonNode inbound = graph(FOCUS_PATH).at("/focusNote/inboundReferences");

    assertThat(inbound.size(), equalTo(3));
    assertThat(inbound.get(0).has("referrerPropertyTypes"), is(false));
    assertThat(inbound.get(1).get("referrerPropertyTypes").toString(), equalTo("[\"see also\"]"));
    assertRoundTrip(inbound.get(1).get("link").asString(), sourceContent);
    assertThat(inbound.get(2).has("referrerPropertyTypes"), is(false));
    assertRoundTrip(inbound.get(2).get("link").asString(), relationContent);
  }

  @ParameterizedTest
  @CsvSource({
    "Outgoing target, outgoing",
    "Inbound target links to [[Engineering:/Focus]]., inbound",
    "Root peer target, peer"
  })
  void retrievalPathSelectsFocusAndEveryTraversedTarget(String content, String route)
      throws Exception {
    JsonNode graph = graph(FOCUS_PATH);
    JsonNode related = null;
    for (JsonNode note : graph.get("relatedNotes")) {
      if (note.get("content").asString().equals(content)) {
        related = note;
        break;
      }
    }
    List<String> expected =
        switch (route) {
          case "outgoing" -> List.of(FOCUS_PATH, OUTGOING_PATH);
          case "inbound" -> List.of(FOCUS_PATH, INBOUND_PATH);
          default -> List.of(FOCUS_PATH);
        };
    assertThat(related.get("retrievalPath").size(), equalTo(expected.size()));
    for (int i = 0; i < expected.size(); i++) {
      String wiki = related.get("retrievalPath").get(i).asString();
      assertThat(wiki, equalTo("[[" + expected.get(i) + "]]"));
      assertRoundTrip(wiki, i == 0 ? focusContent : content);
    }
  }

  private void assertRoundTrip(String wiki, String content) throws Exception {
    String destination =
        WikiLinkMarkdown.splitInner(
                WikiLinkMarkdown.authoredTokensInOccurrenceOrder(wiki).getFirst())
            .rawTarget();
    assertThat(
        call("read_note", Map.of("portablePath", destination)).get("content").asString(),
        equalTo(content));
    assertThat(graph(destination).at("/focusNote/content").asString(), equalTo(content));
  }

  private JsonNode graph(String destination) throws Exception {
    return call("get_note_graph", Map.of("portablePath", destination, "tokenLimit", 2000));
  }

  private JsonNode call(String name, Map<String, Object> arguments) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "jsonrpc",
                "2.0",
                "id",
                3,
                "method",
                "tools/call",
                "params",
                Map.of("name", name, "arguments", arguments)));
    var response =
        mockMvc
            .perform(authorized(mcpPost(body)))
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
