package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerStalePropertyWikiLinkTests extends TextContentControllerTestBase {

  @Autowired NoteController noteController;

  @Test
  void removingTargetPropertyLeavesReferringPropertyWikiUnresolved()
      throws UnexpectedNoAccessRightException {
    assertReferringPropertyWikiUnresolvedAfterMoonContent("Moon body.");
  }

  @Test
  void renamingTargetPropertyLeavesOldReferringPropertyWikiUnresolved()
      throws UnexpectedNoAccessRightException {
    assertReferringPropertyWikiUnresolvedAfterMoonContent(
        Frontmatter.empty().set("subject", "v").fenced("Moon body."));
  }

  @Test
  void removingSelfTargetedPropertyLeavesPropertyWikiUnresolved()
      throws UnexpectedNoAccessRightException {
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(currentUser.getUser()).please();
    controller.updateNoteContent(
        moon,
        contentDto(
            Frontmatter.empty().set("a part of", "v").fenced("[[Moon#prop:a%20part%20of]]")));

    controller.updateNoteContent(moon, contentDto("[[Moon#prop:a%20part%20of]]"));

    assertThat(noteController.showNote(moon).getWikiLinks(), empty());
  }

  @Test
  void removingTargetPropertyKeepsNoteOnlyWikiLive() throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();
    controller.updateNoteContent(carrier, contentDto("[[Moon]] [[Moon#prop:a%20part%20of]]"));

    controller.updateNoteContent(moon, contentDto("Moon body."));

    NoteRealm shown = noteController.showNote(carrier);
    assertThat(shown.getWikiLinks(), hasSize(1));
    assertThat(shown.getWikiLinks().getFirst().getAuthoredLink(), equalTo("Moon"));
  }

  private void assertReferringPropertyWikiUnresolvedAfterMoonContent(String moonContent)
      throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();
    controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of]]"));

    controller.updateNoteContent(moon, contentDto(moonContent));

    assertThat(noteController.showNote(carrier).getWikiLinks(), empty());
  }
}
