package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerShowWikiLinkTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private NoteRealm showWithWikiTitles(Note viewer) throws UnexpectedNoAccessRightException {
    return controller.showNote(viewer);
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
    assertThat(wt.getTarget(), equalTo("LinkedPage"));
    assertThat(wt.getDisplayText(), equalTo("LinkedPage"));
    assertThat(wt.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
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
  void shouldResolveWikiLinkTitleIgnoringCase() throws UnexpectedNoAccessRightException {
    Note target =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("LinkedPage").please();
    Note viewer =
        makeMe.aNote().underSameNotebookAs(target).content("Text [[linkedpage]].").please();

    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().getFirst().getDestinationNoteId(),
        equalTo(target.getId()));
  }

  @Test
  void shouldResolveHiraganaAndKatakanaTitlesToTheirDistinctNotes()
      throws UnexpectedNoAccessRightException {
    Note hiragana = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("ごろ").please();
    Note katakana = makeMe.aNote().underSameNotebookAs(hiragana).title("ゴロ").please();
    Note viewer =
        makeMe.aNote().underSameNotebookAs(hiragana).content("Text [[ごろ]] and [[ゴロ]].").please();

    List<WikiLink> wikiLinks = showWithWikiTitles(viewer).getWikiLinks();
    assertThat(
        wikiLinks.stream().map(WikiLink::getDestinationNoteId).toList(),
        contains(hiragana.getId(), katakana.getId()));
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
    assertThat(wt.getTarget(), equalTo("Target Title"));
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
    assertThat(wt.getTarget(), equalTo("Other Notebook:LinkedPage"));
    assertThat(wt.getDestinationNoteId(), equalTo(targetInOther.getId()));
  }

  @Test
  void shouldResolveWikiLinkCreatedAfterOriginalNoteWithoutRefreshingOriginalNote()
      throws UnexpectedNoAccessRightException {
    Note viewer =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("See [[Future]].").please();
    NoteRealm before = controller.showNote(viewer);
    assertThat(before.getWikiLinks(), hasSize(0));

    Note future = makeMe.aNote().underSameNotebookAs(viewer).title("Future").please();

    NoteRealm after = controller.showNote(viewer);
    assertThat(after.getWikiLinks(), hasSize(1));
    WikiLink wt = after.getWikiLinks().get(0);
    assertThat(wt.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(wt.getDestinationNoteId(), equalTo(future.getId()));
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

  @Test
  void shouldKeepSeparateWikiLinksForMultipleDisplayLabelsToSameTarget()
      throws UnexpectedNoAccessRightException {
    Note shared = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Same").please();
    Note viewer =
        makeMe
            .aNote()
            .underSameNotebookAs(shared)
            .content("[[Same|first label]] and [[Same|second label]]")
            .please();

    List<WikiLink> wikiLinks = showWithWikiTitles(viewer).getWikiLinks();
    assertThat(wikiLinks, hasSize(2));
    assertThat(wikiLinks.get(0).getDisplayText(), equalTo("first label"));
    assertThat(wikiLinks.get(1).getDisplayText(), equalTo("second label"));
  }

  @Test
  void shouldOmitFileLookingMarkdownHrefFromWikiLinks() throws UnexpectedNoAccessRightException {
    var folder = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("Folder").please();
    makeMe.aNote().title("Title").folder(folder).please();
    Note viewer =
        makeMe.aNote().notebook(folder.getNotebook()).content("[label](/Folder/Title.md)").please();

    assertThat(showWithWikiTitles(viewer).getWikiLinks(), empty());
  }
}
