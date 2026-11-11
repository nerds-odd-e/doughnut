package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteControllerShowWikiLinkTitlePrecedenceTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired TextContentController textContentController;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void selectsTitleBeforeAliasesAcrossAuthoredLinkForms() throws UnexpectedNoAccessRightException {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Colors").please();
    Note title = makeMe.aNote().notebook(notebook).title("color").please();
    makeMe.aNote().notebook(notebook).title("colour").aliases("color").please();
    makeMe.aNote().notebook(notebook).title("shade").aliases("color").please();
    String content =
        """
        ---
        scalar: '[[color|Scalar]]'
        list:
          - '[[color|List]]'
        ---
        [[color]] [[color|Read this]] [[Colors:color]] [[COLOR|Case]]
        """;
    Note source = makeMe.aNote().notebook(notebook).content(content).please();

    NoteRealm realm = controller.showNote(source);

    assertThat(realm.getWikiLinks(), hasSize(6));
    assertThat(
        realm.getWikiLinks().stream().map(WikiLink::getDestinationNoteId).toList(),
        everyItem(equalTo(title.getId())));
    assertThat(
        realm.getWikiLinks().stream().map(WikiLink::getAuthoredLink).toList(),
        containsInAnyOrder(
            "color|Scalar",
            "color|List",
            "color",
            "color|Read this",
            "Colors:color",
            "COLOR|Case"));
    assertThat(
        realm.getWikiLinks().stream().map(WikiLink::getDisplayText).toList(),
        containsInAnyOrder("Scalar", "List", "color", "Read this", "Colors:color", "Case"));
    assertThat(realm.getNote().getContent(), equalTo(content));
  }

  @Test
  void unreadableTitleLeavesReadableAliasAsDestination() throws UnexpectedNoAccessRightException {
    Notebook hidden =
        makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).name("Colors").please();
    makeMe.aNote().notebook(hidden).title("color").please();
    Notebook visible =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Colors").please();
    Note alias = makeMe.aNote().notebook(visible).title("colour").aliases("color").please();
    Note source =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("[[Colors:color]]").please();

    assertThat(
        controller.showNote(source).getWikiLinks().getFirst().getDestinationNoteId(),
        equalTo(alias.getId()));
  }

  @Test
  void trashedTitleLeavesAvailableAliasAsDestination() throws UnexpectedNoAccessRightException {
    Note alias =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("colour")
            .aliases("color")
            .please();
    makeMe.aNote().notebook(alias.getNotebook()).title("color").trashed().please();
    Note source = makeMe.aNote().underSameNotebookAs(alias).content("[[color]]").please();

    assertThat(
        controller.showNote(source).getWikiLinks().getFirst().getDestinationNoteId(),
        equalTo(alias.getId()));
  }

  @Test
  void selectedTitleResolvesItsExactProperty() throws UnexpectedNoAccessRightException {
    Note title =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("color")
            .content("---\ntopic: title value\n---\n")
            .please();
    makeMe.aNote().underSameNotebookAs(title).title("colour").aliases("color").please();
    Note source =
        makeMe.aNote().underSameNotebookAs(title).content("[[color#prop:topic|Topic]]").please();

    WikiLink resolved = controller.showNote(source).getWikiLinks().getFirst();
    assertThat(resolved.getDestinationNoteId(), equalTo(title.getId()));
    assertThat(resolved.getTarget(), equalTo("color#prop:topic"));
  }

  @Test
  void selectedTitleWithoutPropertyLeavesLinkUnresolvedDespiteAliasProperty()
      throws UnexpectedNoAccessRightException {
    Note title = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("color").please();
    Note alias = makeMe.aNote().underSameNotebookAs(title).title("colour").please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\naliases: [color]\ntopic: alias value\n---\n");
    textContentController.updateNoteContent(alias, content);
    Note source =
        makeMe.aNote().underSameNotebookAs(title).content("[[color#prop:topic]]").please();

    assertThat(controller.showNote(source).getWikiLinks(), empty());
  }

  @Test
  void multipleTitlesRemainAmbiguousBeforePropertiesAreChecked()
      throws UnexpectedNoAccessRightException {
    Note title =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("color")
            .content("---\ntopic: title value\n---\n")
            .please();
    Folder folder = makeMe.aFolder().notebook(title.getNotebook()).please();
    makeMe.aNote().folder(folder).title("color").please();
    makeMe.aNote().underSameNotebookAs(title).title("colour").aliases("color").please();
    Note source =
        makeMe.aNote().underSameNotebookAs(title).content("[[color#prop:topic]]").please();

    assertThat(
        controller.showNote(source).getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }

  @Test
  void unmatchedShorthandRemainsUnresolved() throws UnexpectedNoAccessRightException {
    Note source =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("[[color]]").please();

    assertThat(controller.showNote(source).getWikiLinks(), empty());
  }
}
