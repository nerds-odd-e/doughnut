package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerShowWikiLinkAmbiguityTests extends ControllerTestBase {
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
  void shouldEmitAmbiguousWhenSeveralReadableNotesShareDisplayName()
      throws UnexpectedNoAccessRightException {
    Folder recipes =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("WikiDup Recipes").please();
    Folder pantry =
        makeMe.aFolder().notebook(recipes.getNotebook()).name("WikiDup Pantry").please();
    makeMe.aNote().title("WikiDup Shared").folder(recipes).please();
    makeMe.aNote().title("WikiDup Shared").folder(pantry).please();
    Note viewer =
        makeMe.aNote().notebook(recipes.getNotebook()).content("See [[WikiDup Shared]].").please();
    NoteRealm realm = showWithWikiTitles(viewer);
    assertThat(realm.getWikiLinks(), hasSize(1));
    WikiLink wt = realm.getWikiLinks().get(0);
    assertThat(wt.getAuthoredLink(), equalTo("WikiDup Shared"));
    assertThat(wt.getTarget(), equalTo("WikiDup Shared"));
    assertThat(wt.getDisplayText(), equalTo("WikiDup Shared"));
    assertThat(wt.getResolution(), equalTo(WikiLink.Resolution.AMBIGUOUS));
    assertThat(wt.getDestinationNoteId(), nullValue());
  }

  @Test
  void shouldEmitAmbiguousWhenTitleCollidesWithAlias() throws UnexpectedNoAccessRightException {
    Note byTitle = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("color").please();
    makeMe.aNote().underSameNotebookAs(byTitle).title("colour").aliases("color").please();
    Note viewer = makeMe.aNote().underSameNotebookAs(byTitle).content("Text [[color]].").please();
    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }

  @Test
  void shouldEmitAmbiguousWhenTwoNotesShareAnAlias() throws UnexpectedNoAccessRightException {
    Note first =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("first")
            .aliases("color")
            .please();
    makeMe.aNote().underSameNotebookAs(first).title("second").aliases("color").please();
    Note viewer = makeMe.aNote().underSameNotebookAs(first).content("Text [[color]].").please();
    assertThat(
        showWithWikiTitles(viewer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }

  @Test
  void shouldReflectNewAmbiguityForCachedRowWhenCollidingNoteAddedAfterRefresh()
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

    WikiLink beforeCollision = controller.showNote(viewer).getWikiLinks().get(0);
    assertThat(beforeCollision.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(beforeCollision.getDestinationNoteId(), equalTo(targetInOther.getId()));

    // A colliding note is added without re-saving the referrer.
    Folder anotherFolder = makeMe.aFolder().notebook(otherNotebook).name("Another").please();
    makeMe.aNote().title("LinkedPage").folder(anotherFolder).please();

    WikiLink live = controller.showNote(viewer).getWikiLinks().get(0);
    assertThat(live.getResolution(), equalTo(WikiLink.Resolution.AMBIGUOUS));
    assertThat(live.getDestinationNoteId(), nullValue());
  }

  @Test
  void shouldReflectCurrentViewerCandidateSetRatherThanStaleResolution()
      throws UnexpectedNoAccessRightException {
    Notebook otherNotebookA =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Other Notebook").please();
    Note targetA = makeMe.aNote().title("LinkedPage").notebook(otherNotebookA).please();
    makeMe.aBazaarNotebook(otherNotebookA).please();

    Notebook mainNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Main").please();
    makeMe.aBazaarNotebook(mainNotebook).please();
    Note viewer =
        makeMe
            .aNote()
            .notebook(mainNotebook)
            .content("See [[Other Notebook:LinkedPage]] for more.")
            .please();

    User otherOwner = makeMe.aUser().please();
    Notebook otherNotebookB =
        makeMe.aNotebook().creatorAndOwner(otherOwner).name("Other Notebook").please();
    makeMe.aNote().title("LinkedPage").notebook(otherNotebookB).please();

    // Before the second readable notebook exists, resolution is unique.
    WikiLink beforeSecondNotebook = controller.showNote(viewer).getWikiLinks().get(0);
    assertThat(beforeSecondNotebook.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(beforeSecondNotebook.getDestinationNoteId(), equalTo(targetA.getId()));

    // otherNotebookB becomes readable, and a different viewer (a wider readable candidate set)
    // opens the note.
    makeMe.aBazaarNotebook(otherNotebookB).please();
    currentUser.setUser(makeMe.aUser().please());

    WikiLink live = controller.showNote(viewer).getWikiLinks().get(0);
    assertThat(live.getResolution(), equalTo(WikiLink.Resolution.AMBIGUOUS));
    assertThat(live.getDestinationNoteId(), nullValue());
  }
}
