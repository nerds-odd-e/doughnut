package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.health.HealthRuleIds;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookHealthControllerDeadPropertyWikiLinksTest extends ControllerTestBase {

  @Autowired NotebookHealthController controller;
  @Autowired TextContentController textContentController;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Moon#prop:a%20part%20of", "Moon#prop:%ZZ", "Moon#prop:wikidata"})
  void reportsAbsentInvalidOrMismatchedPropertyTokens(String token)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    makeMe
        .aNote()
        .title("Moon")
        .notebook(notebook)
        .content(Frontmatter.empty().set("WikiData", "v").fenced(""))
        .please();
    makeMe.aNote().title("Linker").notebook(notebook).content("[[" + token + "]]").please();

    HealthFindingGroup group = deadWikiLinksGroup(controller.lint(notebook));
    assertThat(group.getChildren(), hasSize(1));
    assertThat(
        group.getChildren().getFirst().getItems().getFirst().getWikiLinkToken(), equalTo(token));
  }

  @Test
  void doesNotReportLivePropertyToken() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    makeMe
        .aNote()
        .title("Moon")
        .notebook(notebook)
        .content(Frontmatter.empty().set("a part of", "v").fenced(""))
        .please();
    makeMe
        .aNote()
        .title("Linker")
        .notebook(notebook)
        .content("[[Moon#prop:a%20part%20of]]")
        .please();

    assertThat(deadWikiLinksGroup(controller.lint(notebook)).getChildren(), empty());
  }

  @Test
  void reportsMissingCasePropertyTokenWhenCaseSiblingIsLive()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    makeMe
        .aNote()
        .title("Moon")
        .notebook(notebook)
        .content(Frontmatter.empty().set("Name", "v").fenced(""))
        .please();
    makeMe
        .aNote()
        .title("Linker")
        .notebook(notebook)
        .content("[[Moon#prop:Name]] [[Moon#prop:name]]")
        .please();

    assertThat(
        deadWikiLinksGroup(controller.lint(notebook)).getChildren().stream()
            .flatMap(child -> child.getItems().stream())
            .map(HealthFindingItem::getWikiLinkToken)
            .toList(),
        equalTo(List.of("Moon#prop:name")));
  }

  @Test
  void reportsPropertyTokenAfterTargetPropertyIsRemoved() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Note moon =
        makeMe
            .aNote()
            .title("Moon")
            .notebook(notebook)
            .content(Frontmatter.empty().set("a part of", "v").fenced(""))
            .please();
    Note linker = makeMe.aNote().title("Linker").notebook(notebook).please();
    textContentController.updateNoteContent(linker, contentDto("[[Moon#prop:a%20part%20of]]"));

    textContentController.updateNoteContent(moon, contentDto("Moon body."));

    HealthFindingGroup group = deadWikiLinksGroup(controller.lint(notebook));
    assertThat(group.getChildren(), hasSize(1));
    assertThat(
        group.getChildren().getFirst().getItems().getFirst().getWikiLinkToken(),
        equalTo("Moon#prop:a%20part%20of"));
  }

  private Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private HealthFindingGroup deadWikiLinksGroup(NotebookHealthLintReport report) {
    return report.getGroups().stream()
        .filter(g -> HealthRuleIds.DEAD_WIKI_LINKS.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow();
  }

  private NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }
}
