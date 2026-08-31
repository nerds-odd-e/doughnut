package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ResolvedWikiLinkService;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerShowTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private NoteRealm showWithWikiTitles(Note viewer) throws UnexpectedNoAccessRightException {
    resolvedWikiLinkService.refreshForNote(viewer, currentUser.getUser());
    return controller.showNote(viewer);
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showNote(note));
  }

  @Test
  void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    NoteRealm noteRealm = controller.showNote(note);
    assertThat(noteRealm.getId(), equalTo(note.getId()));
    assertThat(noteRealm.getNotebookRealm().readonly(), is(false));
    assertThat(
        noteRealm.getNotebookRealm().notebook().getId(), equalTo(note.getNotebook().getId()));
  }

  @Test
  void shouldReturnReadonlyWhenHavingReadingAuthOnly() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    assertThat(controller.showNote(note).getNotebookRealm().readonly(), is(true));
  }

  @Test
  void shouldReturnWikiTitlesForUnqualifiedLinksByNotebookNameAndTitle()
      throws UnexpectedNoAccessRightException {
    Note matched =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("LinkedPage").please();
    Note viewer =
        makeMe
            .aNote()
            .underSameNotebookAs(matched)
            .content("Text [[LinkedPage]] and [[NoSuch]].")
            .please();
    NoteRealm realm = showWithWikiTitles(viewer);
    assertThat(realm.getWikiLinks(), hasSize(1));
    WikiLink wt = realm.getWikiLinks().get(0);
    assertThat(wt.getAuthoredLink(), equalTo("LinkedPage"));
    assertThat(wt.getPortablePath(), equalTo("LinkedPage"));
    assertThat(wt.getDisplayText(), equalTo("LinkedPage"));
    assertThat(wt.getDestinationNoteId(), equalTo(matched.getId()));
  }

  @Test
  void shouldResolveWikiLinkToSingleFrontmatterAliasTarget()
      throws UnexpectedNoAccessRightException {
    Note aliasTarget =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("colour")
            .aliases("color")
            .please();
    Note viewer =
        makeMe
            .aNote()
            .underSameNotebookAs(aliasTarget)
            .content("Text [[color]] and [[NoSuch]].")
            .please();
    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().get(0).getDestinationNoteId(),
        equalTo(aliasTarget.getId()));
  }

  @Test
  void shouldNotResolveWikiLinkWhenTitleCollidesWithAlias()
      throws UnexpectedNoAccessRightException {
    Note byTitle = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("color").please();
    makeMe.aNote().underSameNotebookAs(byTitle).title("colour").aliases("color").please();
    Note viewer = makeMe.aNote().underSameNotebookAs(byTitle).content("Text [[color]].").please();
    assertThat(showWithWikiTitles(viewer).getWikiLinks(), empty());
  }

  @Test
  void shouldResolveAmbiguousAliasToLowestNoteId() throws UnexpectedNoAccessRightException {
    Note firstTarget =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("first")
            .aliases("color")
            .please();
    makeMe.aNote().underSameNotebookAs(firstTarget).title("second").aliases("color").please();
    Note viewer =
        makeMe.aNote().underSameNotebookAs(firstTarget).content("Text [[color]].").please();
    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().get(0).getDestinationNoteId(),
        equalTo(firstTarget.getId()));
  }

  @Test
  void shouldSkipUnreadableLowestIdAliasCandidateForReadableTarget()
      throws UnexpectedNoAccessRightException {
    User secretOwner = makeMe.aUser().please();
    String sharedNotebookName = "Shared Notebook";
    Notebook secretNotebook =
        makeMe.aNotebook().creatorAndOwner(secretOwner).name(sharedNotebookName).please();
    makeMe.aNote().title("hidden").notebook(secretNotebook).aliases("term").please();

    Notebook readableNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name(sharedNotebookName).please();
    makeMe.aBazaarNotebook(readableNotebook).please();
    Note readableTarget =
        makeMe.aNote().title("visible").notebook(readableNotebook).aliases("term").please();

    Note viewerNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .content("Text [[" + sharedNotebookName + ":term]].")
            .please();
    assertThat(
        showWithWikiTitles(viewerNote).getWikiLinks().get(0).getDestinationNoteId(),
        equalTo(readableTarget.getId()));
  }

  @Test
  void shouldResolveWikiLinkUsingTargetBeforePipeAndExposeDisplayFields()
      throws UnexpectedNoAccessRightException {
    Note matched =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Target Title").please();
    Note viewer =
        makeMe
            .aNote()
            .underSameNotebookAs(matched)
            .content("Text [[Target Title|friendly label]] end.")
            .please();
    WikiLink wt = showWithWikiTitles(viewer).getWikiLinks().get(0);
    assertThat(wt.getAuthoredLink(), equalTo("Target Title|friendly label"));
    assertThat(wt.getPortablePath(), equalTo("Target Title"));
    assertThat(wt.getDisplayText(), equalTo("friendly label"));
    assertThat(wt.getDestinationNoteId(), equalTo(matched.getId()));
  }

  @Test
  void shouldReturnWikiTitlesForQualifiedLinkToNoteInAnotherNotebook()
      throws UnexpectedNoAccessRightException {
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Other Notebook").please();
    Note targetInOther = makeMe.aNote().title("LinkedPage").notebook(otherNotebook).please();
    Notebook mainNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Main").please();
    Note viewer =
        makeMe
            .aNote()
            .notebook(mainNotebook)
            .content("See [[Other Notebook:LinkedPage]] for more.")
            .please();
    WikiLink wt = showWithWikiTitles(viewer).getWikiLinks().get(0);
    assertThat(wt.getPortablePath(), equalTo("Other Notebook:LinkedPage"));
    assertThat(wt.getDestinationNoteId(), equalTo(targetInOther.getId()));
  }

  @Test
  void shouldReturnWikiTitlesFromFrontmatterBlocks() throws UnexpectedNoAccessRightException {
    Note fromFm =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("FrontmatterTarget").please();
    Note viewer =
        makeMe
            .aNote()
            .underSameNotebookAs(fromFm)
            .content(
                """
                ---
                see: Summary with [[FrontmatterTarget]]
                ---
                [[FrontmatterTarget]] body
                """)
            .please();
    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().get(0).getDestinationNoteId(),
        equalTo(fromFm.getId()));
  }
}
